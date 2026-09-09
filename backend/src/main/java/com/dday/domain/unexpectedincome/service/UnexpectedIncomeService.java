package com.dday.domain.unexpectedincome.service;

import com.dday.domain.budget.entity.BudgetChangeDetail;
import com.dday.domain.budget.entity.BudgetChangeHistory;
import com.dday.domain.budget.entity.BudgetChangeType;
import com.dday.domain.budget.entity.MonthlyBudget;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.income.entity.RecurringIncome;
import com.dday.domain.mydata.entity.FinancialTransaction;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.unexpectedincome.dto.request.UnexpectedIncomeAddRequest;
import com.dday.domain.unexpectedincome.dto.request.UnexpectedIncomeAllocationRequest;
import com.dday.domain.unexpectedincome.dto.response.UnexpectedIncomeResponse;
import com.dday.domain.unexpectedincome.repository.UnexpectedIncomeBudgetChangeDetailRepository;
import com.dday.domain.unexpectedincome.repository.UnexpectedIncomeBudgetChangeHistoryRepository;
import com.dday.domain.unexpectedincome.repository.UnexpectedIncomeMonthlyBudgetRepository;
import com.dday.domain.unexpectedincome.repository.UnexpectedIncomeMonthlyPocketBudgetRepository;
import com.dday.domain.unexpectedincome.repository.UnexpectedIncomeRecurringIncomeRepository;
import com.dday.domain.unexpectedincome.repository.UnexpectedIncomeTransactionRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dday.domain.unexpectedincome.dto.UnexpectedIncomeErrorCode.ALLOCATION_AMOUNT_MISMATCH;
import static com.dday.domain.unexpectedincome.dto.UnexpectedIncomeErrorCode.CURRENT_BUDGET_NOT_FOUND;
import static com.dday.domain.unexpectedincome.dto.UnexpectedIncomeErrorCode.INCOME_ALREADY_PROCESSED;
import static com.dday.domain.unexpectedincome.dto.UnexpectedIncomeErrorCode.INCOME_TRANSACTION_NOT_FOUND;
import static com.dday.domain.unexpectedincome.dto.UnexpectedIncomeErrorCode.INVALID_ADD_AMOUNT;
import static com.dday.domain.unexpectedincome.dto.UnexpectedIncomeErrorCode.INVALID_ALLOCATION;
import static com.dday.domain.unexpectedincome.dto.UnexpectedIncomeErrorCode.MONTHLY_POCKET_BUDGET_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UnexpectedIncomeService {

    /*
     * 등록된 고정수입의 예정일과 실제 입금일의 차이가
     * 3일 이내면 입금 시기가 비슷하다고 판단한다.
     */
    private static final int DATE_TOLERANCE_DAYS = 3;

    /*
     * 등록된 고정수입 금액의 5%까지는
     * 금액이 비슷하다고 판단한다.
     */
    private static final double AMOUNT_TOLERANCE_RATE = 0.05;

    /*
     * 금액이 작은 고정수입도 지나치게 엄격하게
     * 비교되지 않도록 최소 5,000원의 오차를 허용한다.
     */
    private static final long MIN_AMOUNT_TOLERANCE = 5_000L;

    /*
     * depositTiming이
     * "매월 10일", "매달 25일", "10일" 등일 때
     * 날짜 숫자를 추출한다.
     */
    private static final Pattern DAY_PATTERN =
            Pattern.compile("(\\d{1,2})\\s*일");

    private final UnexpectedIncomeTransactionRepository transactionRepository;

    private final UnexpectedIncomeRecurringIncomeRepository recurringIncomeRepository;

    private final UnexpectedIncomeMonthlyBudgetRepository monthlyBudgetRepository;

    private final UnexpectedIncomeMonthlyPocketBudgetRepository monthlyPocketBudgetRepository;

    private final UnexpectedIncomeBudgetChangeHistoryRepository budgetChangeHistoryRepository;

    private final UnexpectedIncomeBudgetChangeDetailRepository budgetChangeDetailRepository;

    /**
     * 아직 처리하지 않은 신규 입금 중
     * 가장 먼저 들어온 한 건을 조회한다.
     *
     * 고정수입과 시기 / 금액을 비교하여
     *
     * 1. NEW_INCOME
     * 2. RECURRING_LIKELY
     * 3. RECURRING_OVER
     *
     * 중 하나로 분류한다.
     *
     * 여기서는 절대로 자동 처리하지 않는다.
     * 사용자 선택 전까지 newFundChecked는 false로 유지한다.
     */
    @Transactional(readOnly = true)
    public UnexpectedIncomeResponse findPending(
            Long userId
    ) {
        List<FinancialTransaction> pending =
                transactionRepository.findPendingIncomes(
                        userId,
                        PageRequest.of(0, 1)
                );

        if (pending.isEmpty()) {
            return null;
        }

        FinancialTransaction transaction =
                pending.get(0);

        List<RecurringIncome> recurringIncomes =
                recurringIncomeRepository
                        .findAllByUserUserIdOrderByRecurringIncomeIdAsc(
                                userId
                        );

        return classify(
                transaction,
                recurringIncomes
        );
    }

    /**
     * 사용자가
     * "이번 달 예산에 포함하지 않기"
     * 를 선택했을 때 호출한다.
     *
     * 예산은 변경하지 않고,
     * 동일 입금을 다시 묻지 않도록 처리 완료만 한다.
     */
    @Transactional
    public void exclude(
            Long userId,
            Long transactionId
    ) {
        FinancialTransaction transaction =
                getIncomeForUpdate(
                        userId,
                        transactionId
                );

        validateNotProcessed(transaction);

        transaction.markNewFundChecked();
    }

    /**
     * 신규 입금 중 사용자가 선택한 금액을
     * 이번 달 예산에 추가한다.
     *
     * 일반 신규 입금의 경우:
     * 전체 또는 일부 금액 추가 가능
     *
     * 고정수입 초과 입금의 경우:
     * 프론트에서 excessAmount만 addAmount로 보내면
     * 초과분만 예산에 추가할 수 있다.
     */
    @Transactional
    public void addToBudget(
            Long userId,
            Long transactionId,
            UnexpectedIncomeAddRequest request
    ) {
        FinancialTransaction transaction =
                getIncomeForUpdate(
                        userId,
                        transactionId
                );

        validateNotProcessed(transaction);

        validateAddRequest(
                transaction,
                request
        );

        /*
         * 실제 입금이 발생한 달의 예산을 변경한다.
         */
        LocalDate budgetMonth =
                YearMonth.from(
                        transaction.getTransactionAt()
                ).atDay(1);

        MonthlyBudget monthlyBudget =
                monthlyBudgetRepository
                        .findForUpdate(
                                userId,
                                budgetMonth
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        CURRENT_BUDGET_NOT_FOUND
                                )
                        );

        if (!monthlyBudget.isConfirmed()) {
            throw new BusinessException(
                    CURRENT_BUDGET_NOT_FOUND
            );
        }

        /*
         * 해당 월의 네 포켓 예산을 잠금 조회한다.
         */
        List<MonthlyPocketBudget> pocketBudgets =
                monthlyPocketBudgetRepository
                        .findAllForUpdate(
                                monthlyBudget.getMonthlyBudgetId()
                        );

        if (pocketBudgets.size()
                != PocketType.values().length) {

            throw new BusinessException(
                    MONTHLY_POCKET_BUDGET_NOT_FOUND
            );
        }

        Map<PocketType, Long> allocationMap =
                toAllocationMap(
                        request.getAllocations()
                );

        /*
         * 변경 전/후 총예산
         */
        long previousTotal =
                monthlyBudget.getTotalBudgetAmount();

        long changedTotal =
                previousTotal
                        + request.getAddAmount();

        /*
         * 예산 변경 이력 저장
         */
        BudgetChangeHistory history =
                BudgetChangeHistory.builder()
                        .monthlyBudget(monthlyBudget)
                        .previousTotalBudget(previousTotal)
                        .changedTotalBudget(changedTotal)
                        .changeType(
                                BudgetChangeType.USER_EDIT
                        )
                        .changeReason(
                                "새로 들어온 돈을 이번 달 예산에 추가"
                        )
                        .build();

        budgetChangeHistoryRepository.save(
                history
        );

        /*
         * 각 포켓에 사용자가 선택한 금액을 추가한다.
         */
        for (MonthlyPocketBudget pocketBudget
                : pocketBudgets) {

            PocketType pocketType =
                    pocketBudget
                            .getPocket()
                            .getPocketType();

            Long addedAmount =
                    allocationMap.get(
                            pocketType
                    );

            if (addedAmount == null) {
                throw new BusinessException(
                        INVALID_ALLOCATION
                );
            }

            long previousAmount =
                    pocketBudget.getTargetAmount();

            long changedAmount =
                    previousAmount
                            + addedAmount;

            BudgetChangeDetail detail =
                    BudgetChangeDetail.builder()
                            .budgetChangeHistory(history)
                            .pocket(
                                    pocketBudget.getPocket()
                            )
                            .previousAmount(
                                    previousAmount
                            )
                            .changedAmount(
                                    changedAmount
                            )
                            .build();

            budgetChangeDetailRepository.save(
                    detail
            );

            pocketBudget.changeTargetAmount(
                    changedAmount
            );
        }

        /*
         * 월 총예산 변경
         */
        monthlyBudget.changeTotalAmount(
                changedTotal
        );

        /*
         * 해당 입금 처리 완료
         */
        transaction.markNewFundChecked();
    }

    /**
     * 신규 입금을 세 가지 유형 중 하나로 판정한다.
     */
    private UnexpectedIncomeResponse classify(
            FinancialTransaction transaction,
            List<RecurringIncome> recurringIncomes
    ) {
        /*
         * 등록된 고정수입 자체가 없으면
         * 바로 일반 신규 입금이다.
         */
        if (recurringIncomes.isEmpty()) {
            return UnexpectedIncomeResponse
                    .newIncome(
                            transaction
                    );
        }

        /*
         * 1순위
         *
         * 입금 시기 + 입금 금액이 모두 유사한 경우.
         *
         * 예:
         * 등록 월급 300,000원 / 매월 10일
         * 실제 입금 298,430원 / 9월 10일
         *
         * 사용자가 직접 고정수입 여부를 확인한다.
         */
        Optional<RecurringIncome> likely =
                recurringIncomes
                        .stream()
                        .filter(
                                recurringIncome ->
                                        isTimingSimilar(
                                                transaction,
                                                recurringIncome
                                        )
                        )
                        .filter(
                                recurringIncome ->
                                        isAmountSimilar(
                                                transaction,
                                                recurringIncome
                                        )
                        )
                        .min(
                                (left, right) ->
                                        Long.compare(
                                                amountDifference(
                                                        transaction,
                                                        left
                                                ),
                                                amountDifference(
                                                        transaction,
                                                        right
                                                )
                                        )
                        );

        if (likely.isPresent()) {
            return UnexpectedIncomeResponse
                    .recurringLikely(
                            transaction,
                            likely.get()
                    );
        }

        /*
         * 2순위
         *
         * 시기는 비슷하지만
         * 등록된 고정수입보다 의미 있게 많이 들어온 경우.
         *
         * 예:
         * 등록 월급 300,000원
         * 실제 입금 500,000원
         *
         * 차액인 200,000원을
         * 포켓에 추가할지 사용자가 결정한다.
         */
        Optional<RecurringIncome> over =
                recurringIncomes
                        .stream()
                        .filter(
                                recurringIncome ->
                                        isTimingSimilar(
                                                transaction,
                                                recurringIncome
                                        )
                        )
                        .filter(
                                recurringIncome ->
                                        isMeaningfullyOver(
                                                transaction,
                                                recurringIncome
                                        )
                        )
                        .min(
                                (left, right) ->
                                        Long.compare(
                                                amountDifference(
                                                        transaction,
                                                        left
                                                ),
                                                amountDifference(
                                                        transaction,
                                                        right
                                                )
                                        )
                        );

        if (over.isPresent()) {
            return UnexpectedIncomeResponse
                    .recurringOver(
                            transaction,
                            over.get()
                    );
        }

        /*
         * 다음과 같은 경우는 시스템이
         * 고정수입이라고 억지로 판단하지 않는다.
         *
         * - 완전히 새로운 돈
         * - 고정수입보다 적게 입금
         * - 15만 + 15만 식 분할 입금
         * - 입금 시기가 크게 다른 경우
         *
         * 일반 새 돈 모달에서
         *
         * "입금된 돈이 정기수입이라면
         * 이번 달 예산에 포함하지 않아도 됩니다."
         *
         * 안내 후 사용자가 직접 선택한다.
         */
        return UnexpectedIncomeResponse
                .newIncome(
                        transaction
                );
    }

    /**
     * 등록된 입금 예정일과
     * 실제 입금일이 비슷한지 확인한다.
     */
    private boolean isTimingSimilar(
            FinancialTransaction transaction,
            RecurringIncome recurringIncome
    ) {
        Integer expectedDay =
                extractExpectedDay(
                        recurringIncome.getDepositTiming()
                );

        /*
         * 입금 예정일을 숫자로 해석할 수 없다면
         * 시스템에서 고정수입 추정을 하지 않는다.
         */
        if (expectedDay == null) {
            return false;
        }

        LocalDate transactionDate =
                transaction
                        .getTransactionAt()
                        .toLocalDate();

        int actualDay =
                transactionDate.getDayOfMonth();

        int maxDay =
                transactionDate.lengthOfMonth();

        /*
         * 예를 들어 "매월 31일"인데
         * 2월이라면 그 달 마지막 날로 보정한다.
         */
        int normalizedExpectedDay =
                Math.min(
                        expectedDay,
                        maxDay
                );

        int difference =
                Math.abs(
                        actualDay
                                - normalizedExpectedDay
                );

        /*
         * 단순 날짜 차이와
         * 월말 ↔ 월초 차이를 모두 고려한다.
         *
         * 예:
         * 예정일 31일
         * 실제입금 1일
         *
         * 이런 경우도 날짜가 가까운 것으로 본다.
         */
        int circularDifference =
                Math.min(
                        difference,
                        maxDay - difference
                );

        return circularDifference
                <= DATE_TOLERANCE_DAYS;
    }

    /**
     * 실제 입금액과 등록된 고정수입 금액이
     * 허용 오차 범위 내인지 확인한다.
     */
    private boolean isAmountSimilar(
            FinancialTransaction transaction,
            RecurringIncome recurringIncome
    ) {
        long expected =
                recurringIncome
                        .getExpectedAmount();

        long tolerance =
                calculateAmountTolerance(
                        expected
                );

        long difference =
                Math.abs(
                        transaction.getAmount()
                                - expected
                );

        return difference <= tolerance;
    }

    /**
     * 고정수입보다 의미 있게 많이 입금됐는지 확인한다.
     *
     * 단순 1,000원 정도의 오차 때문에
     * 초과수입 모달을 띄우지 않도록
     * 허용 오차보다 큰 경우만 true.
     */
    private boolean isMeaningfullyOver(
            FinancialTransaction transaction,
            RecurringIncome recurringIncome
    ) {
        long expected =
                recurringIncome
                        .getExpectedAmount();

        long tolerance =
                calculateAmountTolerance(
                        expected
                );

        return transaction.getAmount()
                > expected + tolerance;
    }

    /**
     * 고정수입 금액 비교 허용 오차.
     *
     * expectedAmount의 5%와
     * 5,000원 중 큰 값을 사용한다.
     */
    private long calculateAmountTolerance(
            long expectedAmount
    ) {
        return Math.max(
                MIN_AMOUNT_TOLERANCE,
                Math.round(
                        expectedAmount
                                * AMOUNT_TOLERANCE_RATE
                )
        );
    }

    private long amountDifference(
            FinancialTransaction transaction,
            RecurringIncome recurringIncome
    ) {
        return Math.abs(
                transaction.getAmount()
                        - recurringIncome.getExpectedAmount()
        );
    }

    /**
     * depositTiming 문자열에서
     * 예상 입금일을 추출한다.
     *
     * 지원:
     * 매월 10일
     * 매달 10일
     * 10일
     *
     * 현재 미지원:
     * 격주 금요일
     * 매월 말일
     */
    private Integer extractExpectedDay(
            String depositTiming
    ) {
        if (depositTiming == null
                || depositTiming.isBlank()) {

            return null;
        }

        Matcher matcher =
                DAY_PATTERN.matcher(
                        depositTiming
                );

        if (!matcher.find()) {
            return null;
        }

        int day =
                Integer.parseInt(
                        matcher.group(1)
                );

        if (day < 1 || day > 31) {
            return null;
        }

        return day;
    }

    /**
     * 사용자의 해당 입금 거래를
     * PESSIMISTIC_WRITE로 조회한다.
     */
    private FinancialTransaction getIncomeForUpdate(
            Long userId,
            Long transactionId
    ) {
        return transactionRepository
                .findIncomeForUpdate(
                        userId,
                        transactionId
                )
                .orElseThrow(
                        () -> new BusinessException(
                                INCOME_TRANSACTION_NOT_FOUND
                        )
                );
    }

    /**
     * 이미 사용자가 처리한 입금을
     * 다시 수정하지 못하게 막는다.
     */
    private void validateNotProcessed(
            FinancialTransaction transaction
    ) {
        if (transaction.isNewFundChecked()) {
            throw new BusinessException(
                    INCOME_ALREADY_PROCESSED
            );
        }
    }

    /**
     * 예산에 추가할 금액 및
     * 포켓 배분값을 검증한다.
     */
    private void validateAddRequest(
            FinancialTransaction transaction,
            UnexpectedIncomeAddRequest request
    ) {
        if (request == null
                || request.getAddAmount() == null
                || request.getAddAmount() <= 0
                || request.getAddAmount()
                > transaction.getAmount()) {

            throw new BusinessException(
                    INVALID_ADD_AMOUNT
            );
        }

        if (request.getAllocations() == null
                || request.getAllocations().size()
                != PocketType.values().length) {

            throw new BusinessException(
                    INVALID_ALLOCATION
            );
        }

        Set<PocketType> types =
                new HashSet<>();

        long allocationTotal = 0L;

        for (UnexpectedIncomeAllocationRequest allocation
                : request.getAllocations()) {

            if (allocation == null
                    || allocation.getPocketType() == null
                    || allocation.getAmount() == null
                    || allocation.getAmount() < 0) {

                throw new BusinessException(
                        INVALID_ALLOCATION
                );
            }

            /*
             * 같은 포켓이 두 번 들어오는 것도 막는다.
             */
            if (!types.add(
                    allocation.getPocketType()
            )) {
                throw new BusinessException(
                        INVALID_ALLOCATION
                );
            }

            allocationTotal +=
                    allocation.getAmount();
        }

        /*
         * 반드시 네 종류의 포켓이
         * 정확히 한 번씩 존재해야 한다.
         */
        if (types.size()
                != PocketType.values().length) {

            throw new BusinessException(
                    INVALID_ALLOCATION
            );
        }

        /*
         * 포켓별 추가 금액 합계와
         * 실제 추가할 총금액이 일치해야 한다.
         */
        if (allocationTotal
                != request.getAddAmount()) {

            throw new BusinessException(
                    ALLOCATION_AMOUNT_MISMATCH
            );
        }
    }

    private Map<PocketType, Long> toAllocationMap(
            List<UnexpectedIncomeAllocationRequest> allocations
    ) {
        Map<PocketType, Long> result =
                new EnumMap<>(
                        PocketType.class
                );

        for (UnexpectedIncomeAllocationRequest allocation
                : allocations) {

            result.put(
                    allocation.getPocketType(),
                    allocation.getAmount()
            );
        }

        return result;
    }
}