package com.dday.domain.unexpectedincome.service;

import com.dday.domain.budget.entity.BudgetChangeDetail;
import com.dday.domain.budget.entity.BudgetChangeHistory;
import com.dday.domain.budget.entity.BudgetChangeType;
import com.dday.domain.budget.entity.MonthlyBudget;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.budget.repository.MonthlyBudgetRepository;
import com.dday.domain.income.entity.RecurringIncome;
import com.dday.domain.mydata.entity.FinancialTransaction;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.unexpectedincome.dto.request.UnexpectedIncomeAddRequest;
import com.dday.domain.unexpectedincome.dto.request.UnexpectedIncomeAllocationRequest;
import com.dday.domain.unexpectedincome.dto.response.PendingUnexpectedIncomeResponse;
import com.dday.domain.unexpectedincome.dto.response.UnexpectedIncomeResponse;
import com.dday.domain.unexpectedincome.repository.UnexpectedIncomeBudgetChangeDetailRepository;
import com.dday.domain.unexpectedincome.repository.UnexpectedIncomeBudgetChangeHistoryRepository;
import com.dday.domain.unexpectedincome.repository.UnexpectedIncomeMonthlyPocketBudgetRepository;
import com.dday.domain.unexpectedincome.repository.UnexpectedIncomeRecurringIncomeRepository;
import com.dday.domain.unexpectedincome.repository.UnexpectedIncomeTransactionRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    /*
     * 기존 UnexpectedIncomeMonthlyBudgetRepository 대신
     * 예산 도메인의 공통 MonthlyBudgetRepository를 사용한다.
     */
    private final MonthlyBudgetRepository monthlyBudgetRepository;

    private final UnexpectedIncomeMonthlyPocketBudgetRepository monthlyPocketBudgetRepository;

    private final UnexpectedIncomeBudgetChangeHistoryRepository budgetChangeHistoryRepository;

    private final UnexpectedIncomeBudgetChangeDetailRepository budgetChangeDetailRepository;

    /**
     * 아직 처리하지 않은 신규 입금을 모두 조회한다.
     *
     * 각 입금을 고정수입과 비교하여
     *
     * 1. NEW_INCOME
     * 2. RECURRING_LIKELY
     * 3. RECURRING_OVER
     *
     * 중 하나로 분류한다.
     *
     * 여기서는 자동 처리하지 않는다.
     * 사용자 선택 전까지 newFundChecked는 false로 유지한다.
     */
    @Transactional(readOnly = true)
    public PendingUnexpectedIncomeResponse findPending(
            Long userId
    ) {
        List<FinancialTransaction> pending =
                transactionRepository.findPendingIncomes(
                        userId
                );

        if (pending.isEmpty()) {
            return PendingUnexpectedIncomeResponse.of(
                    List.of()
            );
        }

        List<RecurringIncome> recurringIncomes =
                recurringIncomeRepository
                        .findAllByUserUserIdOrderByRecurringIncomeIdAsc(
                                userId
                        );

        List<UnexpectedIncomeResponse> incomes =
                pending.stream()
                        .map(
                                transaction ->
                                        classify(
                                                transaction,
                                                recurringIncomes
                                        )
                        )
                        .toList();

        return PendingUnexpectedIncomeResponse.of(
                incomes
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

        validateNotProcessed(
                transaction
        );

        transaction.markNewFundChecked();
    }

    /**
     * 신규 입금 중 사용자가 선택한 금액을
     * 이번 달 예산에 추가한다.
     *
     * 일반 신규 입금:
     * 전체 또는 일부 금액 추가 가능
     *
     * 고정수입 초과 입금:
     * 프론트에서 excessAmount를 addAmount로 보내면
     * 초과분만 예산에 추가 가능
     */
    @Transactional
    public void addToBudget(
            Long userId,
            Long transactionId,
            UnexpectedIncomeAddRequest request
    ) {
        /*
         * 현재 로그인 사용자의 입금 거래인지 확인하고
         * 동시에 중복 처리 방지를 위해 잠금 조회한다.
         */
        FinancialTransaction transaction =
                getIncomeForUpdate(
                        userId,
                        transactionId
                );

        validateNotProcessed(
                transaction
        );

        validateAddRequest(
                transaction,
                request
        );

        /*
         * 이 기능의 의미는
         * "이번 달 예산에 추가"이다.
         *
         * 따라서 거래 발생월이 아니라
         * 현재 월 예산을 변경한다.
         *
         * 예:
         * 현재가 2026-09-10이면
         * budgetMonth = 2026-09-01
         */
        LocalDate budgetMonth =
                LocalDate
                        .now()
                        .withDayOfMonth(1);

        /*
         * 현재 사용자의 이번 달 CONFIRMED 예산을
         * PESSIMISTIC_WRITE로 조회한다.
         */
        MonthlyBudget monthlyBudget =
                monthlyBudgetRepository
                        .findConfirmedForUpdate(
                                userId,
                                budgetMonth
                        )
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                CURRENT_BUDGET_NOT_FOUND
                                        )
                        );

        /*
         * 해당 월의 네 포켓 예산을
         * 잠금 상태로 모두 가져온다.
         */
        List<MonthlyPocketBudget> pocketBudgets =
                monthlyPocketBudgetRepository
                        .findAllForUpdate(
                                monthlyBudget
                                        .getMonthlyBudgetId()
                        );

        /*
         * 필수 / 자유 / 미래자산 / 비상금
         * 네 포켓이 전부 존재해야 한다.
         */
        if (
                pocketBudgets.size()
                        != PocketType.values().length
        ) {
            throw new BusinessException(
                    MONTHLY_POCKET_BUDGET_NOT_FOUND
            );
        }

        /*
         * 프론트에서 받은 포켓별 추가 금액을
         * PocketType -> amount 형태로 변환한다.
         */
        Map<PocketType, Long> allocationMap =
                toAllocationMap(
                        request.getAllocations()
                );

        /*
         * 변경 전 월 총예산.
         */
        long previousTotal =
                monthlyBudget
                        .getTotalBudgetAmount();

        /*
         * 변경 후 월 총예산.
         */
        long changedTotal =
                previousTotal
                        + request.getAddAmount();

        /*
         * 예산 변경 이력 저장.
         */
        BudgetChangeHistory history =
                BudgetChangeHistory
                        .builder()
                        .monthlyBudget(
                                monthlyBudget
                        )
                        .previousTotalBudget(
                                previousTotal
                        )
                        .changedTotalBudget(
                                changedTotal
                        )
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
         * 각 포켓의 기존 목표 금액에
         * 사용자가 지정한 추가 금액을 더한다.
         */
        for (
                MonthlyPocketBudget pocketBudget
                : pocketBudgets
        ) {
            PocketType pocketType =
                    pocketBudget
                            .getPocket()
                            .getPocketType();

            Long addedAmount =
                    allocationMap.get(
                            pocketType
                    );

            /*
             * 네 포켓 중 하나라도 요청에서 빠졌다면
             * 잘못된 배분 요청으로 처리한다.
             */
            if (addedAmount == null) {
                throw new BusinessException(
                        INVALID_ALLOCATION
                );
            }

            long previousAmount =
                    pocketBudget
                            .getTargetAmount();

            long changedAmount =
                    previousAmount
                            + addedAmount;

            /*
             * 포켓별 변경 이력 저장.
             */
            BudgetChangeDetail detail =
                    BudgetChangeDetail
                            .builder()
                            .budgetChangeHistory(
                                    history
                            )
                            .pocket(
                                    pocketBudget
                                            .getPocket()
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

            /*
             * 실제 포켓 목표금액 변경.
             */
            pocketBudget.changeTargetAmount(
                    changedAmount
            );
        }

        /*
         * 월 전체 예산도
         * 추가한 입금액만큼 증가시킨다.
         */
        monthlyBudget.changeTotalAmount(
                changedTotal
        );

        /*
         * 같은 입금을 다시 모달로 띄우지 않도록
         * 처리 완료 상태로 변경한다.
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
         * 고정수입이라고 확실히 판단하기 어려우면
         * 일반 신규 입금으로 처리한다.
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
                        recurringIncome
                                .getDepositTiming()
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
                transactionDate
                        .getDayOfMonth();

        int maxDay =
                transactionDate
                        .lengthOfMonth();

        /*
         * 예:
         * 예정일 31일인데 2월이면
         * 해당 월 마지막 날로 보정.
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
         * 월말 ↔ 월초도 가까운 날짜로 본다.
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
                        transaction
                                .getAmount()
                                - expected
                );

        return difference
                <= tolerance;
    }

    /**
     * 고정수입보다 의미 있게
     * 많이 입금됐는지 확인한다.
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

        return transaction
                .getAmount()
                > expected + tolerance;
    }

    /**
     * 고정수입 금액 비교 허용 오차.
     *
     * 예상 금액의 5%와
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

    /**
     * 실제 입금액과
     * 고정수입 예상 금액의 차이.
     */
    private long amountDifference(
            FinancialTransaction transaction,
            RecurringIncome recurringIncome
    ) {
        return Math.abs(
                transaction
                        .getAmount()
                        - recurringIncome
                        .getExpectedAmount()
        );
    }

    /**
     * depositTiming 문자열에서
     * 예상 입금일을 추출한다.
     *
     * 지원 예:
     *
     * 매월 10일
     * 매달 10일
     * 10일
     */
    private Integer extractExpectedDay(
            String depositTiming
    ) {
        if (
                depositTiming == null
                        || depositTiming.isBlank()
        ) {
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

        if (
                day < 1
                        || day > 31
        ) {
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
                        () ->
                                new BusinessException(
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
        if (
                transaction
                        .isNewFundChecked()
        ) {
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
        if (
                request == null
                        || request.getAddAmount() == null
                        || request.getAddAmount() <= 0
                        || request.getAddAmount()
                        > transaction.getAmount()
        ) {
            throw new BusinessException(
                    INVALID_ADD_AMOUNT
            );
        }

        if (
                request.getAllocations() == null
                        || request
                        .getAllocations()
                        .size()
                        != PocketType
                        .values()
                        .length
        ) {
            throw new BusinessException(
                    INVALID_ALLOCATION
            );
        }

        Set<PocketType> types =
                new HashSet<>();

        long allocationTotal = 0L;

        for (
                UnexpectedIncomeAllocationRequest allocation
                : request.getAllocations()
        ) {
            if (
                    allocation == null
                            || allocation
                            .getPocketType()
                            == null
                            || allocation
                            .getAmount()
                            == null
                            || allocation
                            .getAmount()
                            < 0
            ) {
                throw new BusinessException(
                        INVALID_ALLOCATION
                );
            }

            /*
             * 같은 포켓이 중복으로 들어오는 것도 막는다.
             */
            if (
                    !types.add(
                            allocation
                                    .getPocketType()
                    )
            ) {
                throw new BusinessException(
                        INVALID_ALLOCATION
                );
            }

            allocationTotal +=
                    allocation.getAmount();
        }

        /*
         * 네 종류의 포켓이
         * 정확히 한 번씩 있어야 한다.
         */
        if (
                types.size()
                        != PocketType
                        .values()
                        .length
        ) {
            throw new BusinessException(
                    INVALID_ALLOCATION
            );
        }

        /*
         * 포켓별 추가 금액 합계와
         * 실제 추가할 총금액이 일치해야 한다.
         */
        if (
                allocationTotal
                        != request.getAddAmount()
        ) {
            throw new BusinessException(
                    ALLOCATION_AMOUNT_MISMATCH
            );
        }
    }

    /**
     * allocations를
     * PocketType -> 금액 Map으로 변환한다.
     */
    private Map<PocketType, Long> toAllocationMap(
            List<UnexpectedIncomeAllocationRequest> allocations
    ) {
        Map<PocketType, Long> result =
                new EnumMap<>(
                        PocketType.class
                );

        for (
                UnexpectedIncomeAllocationRequest allocation
                : allocations
        ) {
            result.put(
                    allocation.getPocketType(),
                    allocation.getAmount()
            );
        }

        return result;
    }
}