package com.dday.domain.budgetadjustment.service;

import com.dday.domain.budget.entity.BudgetChangeDetail;
import com.dday.domain.budget.entity.BudgetChangeHistory;
import com.dday.domain.budget.entity.BudgetChangeType;
import com.dday.domain.budget.entity.MonthlyBudget;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.budgetadjustment.dto.BudgetAdjustmentErrorCode;
import com.dday.domain.budgetadjustment.dto.request.BudgetAdjustmentRequest;
import com.dday.domain.budgetadjustment.dto.request.PocketAdjustmentRequest;
import com.dday.domain.budgetadjustment.dto.response.BudgetAdjustmentResponse;
import com.dday.domain.budgetadjustment.dto.response.PocketAdjustmentResponse;
import com.dday.domain.budgetadjustment.repository.BudgetAdjustmentDetailRepository;
import com.dday.domain.budgetadjustment.repository.BudgetAdjustmentHistoryRepository;
import com.dday.domain.budgetadjustment.repository.BudgetAdjustmentMonthlyBudgetRepository;
import com.dday.domain.budgetadjustment.repository.BudgetAdjustmentPocketBudgetRepository;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BudgetAdjustmentService {

    private final BudgetAdjustmentMonthlyBudgetRepository monthlyBudgetRepository;
    private final BudgetAdjustmentPocketBudgetRepository pocketBudgetRepository;
    private final BudgetAdjustmentHistoryRepository historyRepository;
    private final BudgetAdjustmentDetailRepository detailRepository;
    private final FinancialTransactionRepository transactionRepository;

    /**
     * 이번 달의 현재 예산과
     * 포켓별 배정액 / 실제 사용액을 조회한다.
     */
    @Transactional(readOnly = true)
    public BudgetAdjustmentResponse findCurrent(Long userId) {

        YearMonth currentMonth = YearMonth.now();

        LocalDate budgetMonth = currentMonth.atDay(1);

        MonthlyBudget monthlyBudget = monthlyBudgetRepository
                .findCurrent(userId, budgetMonth)
                .orElseThrow(() ->
                        new BusinessException(
                                BudgetAdjustmentErrorCode.CURRENT_BUDGET_NOT_FOUND
                        )
                );

        if (!monthlyBudget.isConfirmed()) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode.BUDGET_NOT_CONFIRMED
            );
        }

        List<MonthlyPocketBudget> pocketBudgets =
                pocketBudgetRepository.findAllCurrent(
                        monthlyBudget.getMonthlyBudgetId(),
                        userId
                );

        validatePocketBudgetCount(pocketBudgets);

        Map<Long, Long> spendingMap =
                findSpendingByPocket(userId, currentMonth);

        return createResponse(
                monthlyBudget,
                pocketBudgets,
                spendingMap
        );
    }

    /**
     * 진행 중인 이번 달 예산을 변경한다.
     *
     * 조건
     * 1. 총예산은 0보다 커야 한다.
     * 2. 네 포켓이 모두 존재해야 한다.
     * 3. 포켓 합계 = 총예산이어야 한다.
     * 4. 각 포켓은 이미 사용한 금액보다 낮출 수 없다.
     * 5. 총예산 역시 이미 사용한 총액보다 낮출 수 없다.
     */
    @Transactional
    public BudgetAdjustmentResponse adjust(
            Long userId,
            BudgetAdjustmentRequest request
    ) {

        validateRequest(request);

        YearMonth currentMonth = YearMonth.now();

        LocalDate budgetMonth = currentMonth.atDay(1);

        MonthlyBudget monthlyBudget = monthlyBudgetRepository
                .findCurrentForUpdate(userId, budgetMonth)
                .orElseThrow(() ->
                        new BusinessException(
                                BudgetAdjustmentErrorCode.CURRENT_BUDGET_NOT_FOUND
                        )
                );

        if (!monthlyBudget.isConfirmed()) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode.BUDGET_NOT_CONFIRMED
            );
        }

        List<MonthlyPocketBudget> pocketBudgets =
                pocketBudgetRepository.findAllCurrentForUpdate(
                        monthlyBudget.getMonthlyBudgetId(),
                        userId
                );

        validatePocketBudgetCount(pocketBudgets);

        Map<Long, Long> spendingMap =
                findSpendingByPocket(userId, currentMonth);

        Map<PocketType, Long> requestedAmounts =
                createRequestedAmountMap(request.getAllocations());

        validateTotalAmount(
                request.getTotalBudgetAmount(),
                requestedAmounts
        );

        validateSpentAmounts(
                pocketBudgets,
                spendingMap,
                request.getTotalBudgetAmount(),
                requestedAmounts
        );

        Long previousTotalBudget =
                monthlyBudget.getTotalBudgetAmount();

        Map<PocketType, Long> previousPocketAmounts =
                new EnumMap<>(PocketType.class);

        for (MonthlyPocketBudget pocketBudget : pocketBudgets) {
            previousPocketAmounts.put(
                    pocketBudget.getPocket().getPocketType(),
                    pocketBudget.getTargetAmount()
            );
        }

        boolean totalChanged =
                !previousTotalBudget.equals(
                        request.getTotalBudgetAmount()
                );

        boolean pocketChanged = false;

        for (PocketType pocketType : PocketType.values()) {

            Long previous =
                    previousPocketAmounts.get(pocketType);

            Long changed =
                    requestedAmounts.get(pocketType);

            if (!previous.equals(changed)) {
                pocketChanged = true;
                break;
            }
        }

        /*
         * 값이 실제로 바뀌지 않았다면
         * 변경 이력을 만들 필요가 없다.
         */
        if (!totalChanged && !pocketChanged) {
            return createResponse(
                    monthlyBudget,
                    pocketBudgets,
                    spendingMap
            );
        }

        monthlyBudget.changeTotalAmount(
                request.getTotalBudgetAmount()
        );

        for (MonthlyPocketBudget pocketBudget : pocketBudgets) {

            PocketType pocketType =
                    pocketBudget.getPocket().getPocketType();

            Long changedAmount =
                    requestedAmounts.get(pocketType);

            pocketBudget.changeTargetAmount(changedAmount);
        }

        BudgetChangeType changeType =
                totalChanged
                        ? BudgetChangeType.USER_EDIT
                        : BudgetChangeType.REALLOCATION;

        BudgetChangeHistory history =
                historyRepository.save(
                        BudgetChangeHistory.builder()
                                .monthlyBudget(monthlyBudget)
                                .previousTotalBudget(previousTotalBudget)
                                .changedTotalBudget(
                                        request.getTotalBudgetAmount()
                                )
                                .changeType(changeType)
                                .changeReason("진행 중 포켓 예산 조정")
                                .build()
                );

        List<BudgetChangeDetail> details =
                new ArrayList<>();

        for (MonthlyPocketBudget pocketBudget : pocketBudgets) {

            PocketType pocketType =
                    pocketBudget.getPocket().getPocketType();

            Long previousAmount =
                    previousPocketAmounts.get(pocketType);

            Long changedAmount =
                    requestedAmounts.get(pocketType);

            /*
             * 실제로 변경된 포켓만 상세 이력을 남긴다.
             */
            if (!previousAmount.equals(changedAmount)) {

                details.add(
                        BudgetChangeDetail.builder()
                                .budgetChangeHistory(history)
                                .pocket(pocketBudget.getPocket())
                                .previousAmount(previousAmount)
                                .changedAmount(changedAmount)
                                .build()
                );
            }
        }

        if (!details.isEmpty()) {
            detailRepository.saveAll(details);
        }

        return createResponse(
                monthlyBudget,
                pocketBudgets,
                spendingMap
        );
    }

    private void validateRequest(
            BudgetAdjustmentRequest request
    ) {

        if (request == null
                || request.getTotalBudgetAmount() == null
                || request.getTotalBudgetAmount() <= 0L) {

            throw new BusinessException(
                    BudgetAdjustmentErrorCode.INVALID_TOTAL_BUDGET
            );
        }

        if (request.getAllocations() == null
                || request.getAllocations().size()
                != PocketType.values().length) {

            throw new BusinessException(
                    BudgetAdjustmentErrorCode.INVALID_ALLOCATION
            );
        }
    }

    private Map<PocketType, Long> createRequestedAmountMap(
            List<PocketAdjustmentRequest> allocations
    ) {

        Map<PocketType, Long> amountMap =
                new EnumMap<>(PocketType.class);

        Set<PocketType> pocketTypes =
                new HashSet<>();

        for (PocketAdjustmentRequest allocation : allocations) {

            if (allocation == null
                    || allocation.getPocketType() == null
                    || allocation.getAmount() == null
                    || allocation.getAmount() < 0L) {

                throw new BusinessException(
                        BudgetAdjustmentErrorCode.INVALID_ALLOCATION
                );
            }

            if (!pocketTypes.add(
                    allocation.getPocketType()
            )) {

                throw new BusinessException(
                        BudgetAdjustmentErrorCode.DUPLICATED_POCKET_TYPE
                );
            }

            amountMap.put(
                    allocation.getPocketType(),
                    allocation.getAmount()
            );
        }

        /*
         * 정확히 네 종류가 전부 들어왔는지 확인
         */
        for (PocketType pocketType : PocketType.values()) {

            if (!amountMap.containsKey(pocketType)) {

                throw new BusinessException(
                        BudgetAdjustmentErrorCode.INVALID_ALLOCATION
                );
            }
        }

        return amountMap;
    }

    private void validateTotalAmount(
            Long totalBudgetAmount,
            Map<PocketType, Long> requestedAmounts
    ) {

        long allocationTotal =
                requestedAmounts.values()
                        .stream()
                        .mapToLong(Long::longValue)
                        .sum();

        if (allocationTotal != totalBudgetAmount) {

            throw new BusinessException(
                    BudgetAdjustmentErrorCode.ALLOCATION_AMOUNT_MISMATCH
            );
        }
    }

    private void validateSpentAmounts(
            List<MonthlyPocketBudget> pocketBudgets,
            Map<Long, Long> spendingMap,
            Long totalBudgetAmount,
            Map<PocketType, Long> requestedAmounts
    ) {

        long totalUsedAmount = 0L;

        for (MonthlyPocketBudget pocketBudget : pocketBudgets) {

            Long pocketId =
                    pocketBudget.getPocket().getPocketId();

            PocketType pocketType =
                    pocketBudget.getPocket().getPocketType();

            Long usedAmount =
                    spendingMap.getOrDefault(
                            pocketId,
                            0L
                    );

            totalUsedAmount += usedAmount;

            Long requestedAmount =
                    requestedAmounts.get(pocketType);

            /*
             * 이미 사용한 금액 아래로
             * 포켓 예산을 내릴 수 없다.
             */
            if (requestedAmount < usedAmount) {

                throw new BusinessException(
                        BudgetAdjustmentErrorCode.BUDGET_BELOW_SPENT_AMOUNT
                );
            }
        }

        /*
         * 총예산 역시 이미 사용한 총액보다
         * 낮아질 수 없다.
         */
        if (totalBudgetAmount < totalUsedAmount) {

            throw new BusinessException(
                    BudgetAdjustmentErrorCode.BUDGET_BELOW_SPENT_AMOUNT
            );
        }
    }

    private void validatePocketBudgetCount(
            List<MonthlyPocketBudget> pocketBudgets
    ) {

        if (pocketBudgets.size()
                != PocketType.values().length) {

            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .MONTHLY_POCKET_BUDGET_NOT_FOUND
            );
        }

        Set<PocketType> types =
                new HashSet<>();

        for (MonthlyPocketBudget pocketBudget
                : pocketBudgets) {

            types.add(
                    pocketBudget.getPocket()
                            .getPocketType()
            );
        }

        if (types.size()
                != PocketType.values().length) {

            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .MONTHLY_POCKET_BUDGET_NOT_FOUND
            );
        }
    }

    private Map<Long, Long> findSpendingByPocket(
            Long userId,
            YearMonth month
    ) {

        LocalDateTime from =
                month.atDay(1)
                        .atStartOfDay();

        LocalDateTime to =
                month.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();

        Map<Long, Long> spendingMap =
                new HashMap<>();

        transactionRepository
                .sumSpendingByPocket(
                        userId,
                        from,
                        to
                )
                .forEach(row ->
                        spendingMap.put(
                                ((Number) row[0]).longValue(),
                                ((Number) row[1]).longValue()
                        )
                );

        return spendingMap;
    }

    private BudgetAdjustmentResponse createResponse(
            MonthlyBudget monthlyBudget,
            List<MonthlyPocketBudget> pocketBudgets,
            Map<Long, Long> spendingMap
    ) {

        List<PocketAdjustmentResponse> pockets =
                pocketBudgets.stream()
                        .sorted(
                                Comparator.comparingInt(
                                        budget ->
                                                budget.getPocket()
                                                        .getPocketType()
                                                        .ordinal()
                                )
                        )
                        .map(budget -> {

                            Long usedAmount =
                                    spendingMap.getOrDefault(
                                            budget.getPocket()
                                                    .getPocketId(),
                                            0L
                                    );

                            Long remainingAmount =
                                    Math.max(
                                            budget.getTargetAmount()
                                                    - usedAmount,
                                            0L
                                    );

                            return PocketAdjustmentResponse
                                    .builder()
                                    .pocketId(
                                            budget.getPocket()
                                                    .getPocketId()
                                    )
                                    .pocketType(
                                            budget.getPocket()
                                                    .getPocketType()
                                    )
                                    .pocketName(
                                            budget.getPocket()
                                                    .getPocketName()
                                    )
                                    .targetAmount(
                                            budget.getTargetAmount()
                                    )
                                    .usedAmount(
                                            usedAmount
                                    )
                                    .remainingAmount(
                                            remainingAmount
                                    )
                                    .build();
                        })
                        .toList();

        long totalUsedAmount =
                pockets.stream()
                        .mapToLong(
                                PocketAdjustmentResponse::getUsedAmount
                        )
                        .sum();

        return BudgetAdjustmentResponse
                .builder()
                .month(
                        YearMonth.from(
                                monthlyBudget.getBudgetMonth()
                        ).toString()
                )
                .totalBudgetAmount(
                        monthlyBudget.getTotalBudgetAmount()
                )
                .totalUsedAmount(totalUsedAmount)
                .pockets(pockets)
                .build();
    }
}