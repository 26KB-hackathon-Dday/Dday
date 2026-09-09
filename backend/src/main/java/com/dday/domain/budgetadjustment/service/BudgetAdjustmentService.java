package com.dday.domain.budgetadjustment.service;

import com.dday.domain.asset.entity.ExecutionStatus;
import com.dday.domain.asset.entity.InvestmentActionType;
import com.dday.domain.budget.dto.request.TotalBudgetUpdateRequest;
import com.dday.domain.budget.dto.response.TotalBudgetUpdateResponse;
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
import com.dday.domain.budgetadjustment.repository.BudgetAdjustmentInvestmentRepository;
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

    private final BudgetAdjustmentInvestmentRepository investmentRepository;

    /**
     * 현재 이번 달 예산 조회.
     */
    @Transactional(readOnly = true)
    public BudgetAdjustmentResponse findCurrent(
            Long userId
    ) {

        YearMonth currentMonth =
                YearMonth.now();

        LocalDate budgetMonth =
                currentMonth.atDay(1);

        MonthlyBudget monthlyBudget =
                monthlyBudgetRepository
                        .findCurrent(
                                userId,
                                budgetMonth
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        BudgetAdjustmentErrorCode
                                                .CURRENT_BUDGET_NOT_FOUND
                                )
                        );

        if (!monthlyBudget.isConfirmed()) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .BUDGET_NOT_CONFIRMED
            );
        }

        List<MonthlyPocketBudget> pocketBudgets =
                pocketBudgetRepository
                        .findAllCurrent(
                                monthlyBudget.getMonthlyBudgetId(),
                                userId
                        );

        validatePocketBudgetCount(
                pocketBudgets
        );

        Map<Long, Long> spendingMap =
                findSpendingByPocket(
                        userId,
                        currentMonth
                );

        Map<PocketType, Long> minimumAmounts =
                calculateMinimumAmounts(
                        userId,
                        currentMonth,
                        pocketBudgets,
                        spendingMap
                );

        return createResponse(
                monthlyBudget,
                pocketBudgets,
                minimumAmounts
        );
    }

    /**
     * 총 예산만 단독 수정.
     */
    @Transactional
    public TotalBudgetUpdateResponse updateTotalBudget(
            Long userId,
            TotalBudgetUpdateRequest request
    ) {

        if (
                request == null
                        || request.getTotalBudgetAmount() == null
                        || request.getTotalBudgetAmount() <= 0
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .INVALID_TOTAL_BUDGET
            );
        }

        YearMonth currentMonth =
                YearMonth.now();

        LocalDate budgetMonth =
                currentMonth.atDay(1);

        MonthlyBudget monthlyBudget =
                monthlyBudgetRepository
                        .findCurrentForUpdate(
                                userId,
                                budgetMonth
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        BudgetAdjustmentErrorCode
                                                .CURRENT_BUDGET_NOT_FOUND
                                )
                        );

        if (!monthlyBudget.isConfirmed()) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .BUDGET_NOT_CONFIRMED
            );
        }

        List<MonthlyPocketBudget> pocketBudgets =
                pocketBudgetRepository
                        .findAllCurrentForUpdate(
                                monthlyBudget.getMonthlyBudgetId(),
                                userId
                        );

        validatePocketBudgetCount(
                pocketBudgets
        );

        Map<Long, Long> spendingMap =
                findSpendingByPocket(
                        userId,
                        currentMonth
                );

        Map<PocketType, Long> minimumAmounts =
                calculateMinimumAmounts(
                        userId,
                        currentMonth,
                        pocketBudgets,
                        spendingMap
                );

        long minimumTotalBudget =
                minimumAmounts
                        .values()
                        .stream()
                        .mapToLong(
                                Long::longValue
                        )
                        .sum();

        long newTotalBudget =
                request.getTotalBudgetAmount();

        /*
         * 최소 총예산 =
         * 필수 실제 사용액
         * + 자유 실제 사용액
         * + 미래자산 실제 달성액
         *
         * 비상금은 최소값 0.
         */
        if (
                newTotalBudget
                        < minimumTotalBudget
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .TOTAL_BUDGET_BELOW_MINIMUM
            );
        }

        Long previousTotalBudget =
                monthlyBudget
                        .getTotalBudgetAmount();

        if (
                !previousTotalBudget.equals(
                        newTotalBudget
                )
        ) {
            monthlyBudget.changeTotalAmount(
                    newTotalBudget
            );

            historyRepository.save(
                    BudgetChangeHistory.builder()
                            .monthlyBudget(
                                    monthlyBudget
                            )
                            .previousTotalBudget(
                                    previousTotalBudget
                            )
                            .changedTotalBudget(
                                    newTotalBudget
                            )
                            .changeType(
                                    BudgetChangeType.USER_EDIT
                            )
                            .changeReason(
                                    "총 예산 수정"
                            )
                            .build()
            );
        }

        return TotalBudgetUpdateResponse
                .builder()
                .budgetMonth(
                        monthlyBudget.getBudgetMonth()
                )
                .totalBudgetAmount(
                        monthlyBudget.getTotalBudgetAmount()
                )
                .minimumTotalBudget(
                        minimumTotalBudget
                )
                .build();
    }

    /**
     * 포켓 재조정 최종 저장.
     */
    @Transactional
    public BudgetAdjustmentResponse adjust(
            Long userId,
            BudgetAdjustmentRequest request
    ) {

        validateRequest(
                request
        );

        YearMonth currentMonth =
                YearMonth.now();

        LocalDate budgetMonth =
                currentMonth.atDay(1);

        MonthlyBudget monthlyBudget =
                monthlyBudgetRepository
                        .findCurrentForUpdate(
                                userId,
                                budgetMonth
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        BudgetAdjustmentErrorCode
                                                .CURRENT_BUDGET_NOT_FOUND
                                )
                        );

        if (!monthlyBudget.isConfirmed()) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .BUDGET_NOT_CONFIRMED
            );
        }

        List<MonthlyPocketBudget> pocketBudgets =
                pocketBudgetRepository
                        .findAllCurrentForUpdate(
                                monthlyBudget.getMonthlyBudgetId(),
                                userId
                        );

        validatePocketBudgetCount(
                pocketBudgets
        );

        Map<PocketType, Long> requestedAmounts =
                createRequestedAmountMap(
                        request.getAllocations()
                );

        validateTotalAmount(
                request.getTotalBudgetAmount(),
                requestedAmounts
        );

        Map<Long, Long> spendingMap =
                findSpendingByPocket(
                        userId,
                        currentMonth
                );

        Map<PocketType, Long> minimumAmounts =
                calculateMinimumAmounts(
                        userId,
                        currentMonth,
                        pocketBudgets,
                        spendingMap
                );

        validateMinimumAmounts(
                request.getTotalBudgetAmount(),
                requestedAmounts,
                minimumAmounts
        );

        Long previousTotalBudget =
                monthlyBudget
                        .getTotalBudgetAmount();

        Map<PocketType, Long> previousPocketAmounts =
                new EnumMap<>(
                        PocketType.class
                );

        for (
                MonthlyPocketBudget pocketBudget
                : pocketBudgets
        ) {
            previousPocketAmounts.put(
                    pocketBudget
                            .getPocket()
                            .getPocketType(),
                    pocketBudget
                            .getTargetAmount()
            );
        }

        boolean totalChanged =
                !previousTotalBudget.equals(
                        request.getTotalBudgetAmount()
                );

        boolean pocketChanged =
                false;

        for (
                PocketType pocketType
                : PocketType.values()
        ) {
            Long previous =
                    previousPocketAmounts.get(
                            pocketType
                    );

            Long changed =
                    requestedAmounts.get(
                            pocketType
                    );

            if (
                    !previous.equals(
                            changed
                    )
            ) {
                pocketChanged = true;

                break;
            }
        }

        if (
                !totalChanged
                        && !pocketChanged
        ) {
            return createResponse(
                    monthlyBudget,
                    pocketBudgets,
                    minimumAmounts
            );
        }

        monthlyBudget.changeTotalAmount(
                request.getTotalBudgetAmount()
        );

        for (
                MonthlyPocketBudget pocketBudget
                : pocketBudgets
        ) {

            PocketType pocketType =
                    pocketBudget
                            .getPocket()
                            .getPocketType();

            Long changedAmount =
                    requestedAmounts.get(
                            pocketType
                    );

            pocketBudget.changeTargetAmount(
                    changedAmount
            );
        }

        BudgetChangeType changeType =
                totalChanged
                        ? BudgetChangeType.USER_EDIT
                        : BudgetChangeType.REALLOCATION;

        BudgetChangeHistory history =
                historyRepository.save(
                        BudgetChangeHistory.builder()
                                .monthlyBudget(
                                        monthlyBudget
                                )
                                .previousTotalBudget(
                                        previousTotalBudget
                                )
                                .changedTotalBudget(
                                        request.getTotalBudgetAmount()
                                )
                                .changeType(
                                        changeType
                                )
                                .changeReason(
                                        "진행 중 포켓 예산 조정"
                                )
                                .build()
                );

        List<BudgetChangeDetail> details =
                new ArrayList<>();

        for (
                MonthlyPocketBudget pocketBudget
                : pocketBudgets
        ) {

            PocketType pocketType =
                    pocketBudget
                            .getPocket()
                            .getPocketType();

            Long previousAmount =
                    previousPocketAmounts.get(
                            pocketType
                    );

            Long changedAmount =
                    requestedAmounts.get(
                            pocketType
                    );

            if (
                    !previousAmount.equals(
                            changedAmount
                    )
            ) {
                details.add(
                        BudgetChangeDetail.builder()
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
                                .build()
                );
            }
        }

        if (!details.isEmpty()) {
            detailRepository.saveAll(
                    details
            );
        }

        /*
         * 저장한 새로운 값 기준으로 응답.
         */
        return createResponse(
                monthlyBudget,
                pocketBudgets,
                minimumAmounts
        );
    }

    /**
     * 요청 기본 검증.
     *
     * 현재 DTO는 pockets가 아니라 allocations를 사용한다.
     */
    private void validateRequest(
            BudgetAdjustmentRequest request
    ) {

        if (
                request == null
                        || request.getTotalBudgetAmount() == null
                        || request.getTotalBudgetAmount() <= 0
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .INVALID_TOTAL_BUDGET
            );
        }

        if (
                request.getAllocations() == null
                        || request
                        .getAllocations()
                        .size()
                        != PocketType.values().length
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .INVALID_ALLOCATION
            );
        }
    }

    private Map<PocketType, Long> createRequestedAmountMap(
            List<PocketAdjustmentRequest> allocations
    ) {

        Map<PocketType, Long> amountMap =
                new EnumMap<>(
                        PocketType.class
                );

        Set<PocketType> pocketTypes =
                new HashSet<>();

        for (
                PocketAdjustmentRequest allocation
                : allocations
        ) {

            if (
                    allocation == null
                            || allocation.getPocketType() == null
                            || allocation.getAmount() == null
                            || allocation.getAmount() < 0
            ) {
                throw new BusinessException(
                        BudgetAdjustmentErrorCode
                                .INVALID_ALLOCATION
                );
            }

            if (
                    !pocketTypes.add(
                            allocation.getPocketType()
                    )
            ) {
                throw new BusinessException(
                        BudgetAdjustmentErrorCode
                                .DUPLICATED_POCKET_TYPE
                );
            }

            amountMap.put(
                    allocation.getPocketType(),
                    allocation.getAmount()
            );
        }

        for (
                PocketType pocketType
                : PocketType.values()
        ) {

            if (
                    !amountMap.containsKey(
                            pocketType
                    )
            ) {
                throw new BusinessException(
                        BudgetAdjustmentErrorCode
                                .INVALID_ALLOCATION
                );
            }
        }

        return amountMap;
    }

    /**
     * 네 포켓 합계 = 총 예산 검사.
     */
    private void validateTotalAmount(
            Long totalBudgetAmount,
            Map<PocketType, Long> requestedAmounts
    ) {

        long allocationTotal =
                requestedAmounts
                        .values()
                        .stream()
                        .mapToLong(
                                Long::longValue
                        )
                        .sum();

        if (
                allocationTotal
                        != totalBudgetAmount
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .ALLOCATION_AMOUNT_MISMATCH
            );
        }
    }

    /**
     * 최소 조정 가능 금액 검사.
     */
    private void validateMinimumAmounts(
            Long totalBudgetAmount,
            Map<PocketType, Long> requestedAmounts,
            Map<PocketType, Long> minimumAmounts
    ) {

        long minimumTotal =
                minimumAmounts
                        .values()
                        .stream()
                        .mapToLong(
                                Long::longValue
                        )
                        .sum();

        if (
                totalBudgetAmount
                        < minimumTotal
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .TOTAL_BUDGET_BELOW_MINIMUM
            );
        }

        for (
                PocketType pocketType
                : PocketType.values()
        ) {

            long requested =
                    requestedAmounts
                            .getOrDefault(
                                    pocketType,
                                    0L
                            );

            long minimum =
                    minimumAmounts
                            .getOrDefault(
                                    pocketType,
                                    0L
                            );

            if (
                    requested
                            < minimum
            ) {
                throw new BusinessException(
                        BudgetAdjustmentErrorCode
                                .BELOW_MINIMUM_AMOUNT
                );
            }
        }
    }

    private void validatePocketBudgetCount(
            List<MonthlyPocketBudget> pocketBudgets
    ) {

        if (
                pocketBudgets.size()
                        != PocketType.values().length
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .MONTHLY_POCKET_BUDGET_NOT_FOUND
            );
        }

        Set<PocketType> types =
                new HashSet<>();

        for (
                MonthlyPocketBudget pocketBudget
                : pocketBudgets
        ) {
            types.add(
                    pocketBudget
                            .getPocket()
                            .getPocketType()
            );
        }

        if (
                types.size()
                        != PocketType.values().length
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .MONTHLY_POCKET_BUDGET_NOT_FOUND
            );
        }
    }

    /**
     * 필수/자유 실제 사용액 조회용.
     */
    private Map<Long, Long> findSpendingByPocket(
            Long userId,
            YearMonth month
    ) {

        LocalDateTime from =
                month
                        .atDay(1)
                        .atStartOfDay();

        LocalDateTime to =
                month
                        .plusMonths(1)
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
                .forEach(
                        row -> spendingMap.put(
                                ((Number) row[0]).longValue(),
                                ((Number) row[1]).longValue()
                        )
                );

        return spendingMap;
    }

    /**
     * 미래자산 이번 달 실제 달성액.
     */
    private long findFutureAssetAchievedAmount(
            Long userId,
            YearMonth month
    ) {

        LocalDateTime from =
                month
                        .atDay(1)
                        .atStartOfDay();

        LocalDateTime to =
                month
                        .plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();

        Long inflow =
                investmentRepository
                        .sumAmountByActions(
                                userId,
                                from,
                                to,
                                ExecutionStatus.EXECUTED,
                                Set.of(
                                        InvestmentActionType.BUY,
                                        InvestmentActionType.DEPOSIT
                                )
                        );

        Long outflow =
                investmentRepository
                        .sumAmountByActions(
                                userId,
                                from,
                                to,
                                ExecutionStatus.EXECUTED,
                                Set.of(
                                        InvestmentActionType.SELL,
                                        InvestmentActionType.WITHDRAW
                                )
                        );

        long increased =
                inflow == null
                        ? 0L
                        : inflow;

        long decreased =
                outflow == null
                        ? 0L
                        : outflow;

        return Math.max(
                increased - decreased,
                0L
        );
    }

    /**
     * 포켓별 실제 최소값.
     *
     * ESSENTIAL = 실제 사용액
     * FREE = 실제 사용액
     * FUTURE_ASSET = 실제 달성액
     * EMERGENCY = 0
     */
    private Map<PocketType, Long> calculateMinimumAmounts(
            Long userId,
            YearMonth month,
            List<MonthlyPocketBudget> pocketBudgets,
            Map<Long, Long> spendingMap
    ) {

        Map<PocketType, Long> minimumAmounts =
                new EnumMap<>(
                        PocketType.class
                );

        long futureAchieved =
                findFutureAssetAchievedAmount(
                        userId,
                        month
                );

        for (
                MonthlyPocketBudget pocketBudget
                : pocketBudgets
        ) {

            PocketType pocketType =
                    pocketBudget
                            .getPocket()
                            .getPocketType();

            Long pocketId =
                    pocketBudget
                            .getPocket()
                            .getPocketId();

            long minimum;

            if (
                    pocketType
                            == PocketType.FUTURE_ASSET
            ) {
                minimum =
                        futureAchieved;
            } else if (
                    pocketType
                            == PocketType.EMERGENCY
            ) {
                minimum =
                        0L;
            } else {
                minimum =
                        spendingMap
                                .getOrDefault(
                                        pocketId,
                                        0L
                                );
            }

            minimumAmounts.put(
                    pocketType,
                    minimum
            );
        }

        return minimumAmounts;
    }

    /**
     * 프론트 응답.
     *
     * usedAmount는 화면의 "현재 달성액" 기준으로 사용한다.
     */
    private BudgetAdjustmentResponse createResponse(
            MonthlyBudget monthlyBudget,
            List<MonthlyPocketBudget> pocketBudgets,
            Map<PocketType, Long> minimumAmounts
    ) {

        List<PocketAdjustmentResponse> pockets =
                pocketBudgets
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        budget ->
                                                budget
                                                        .getPocket()
                                                        .getPocketType()
                                                        .ordinal()
                                )
                        )
                        .map(
                                budget -> {

                                    PocketType pocketType =
                                            budget
                                                    .getPocket()
                                                    .getPocketType();

                                    long usedAmount =
                                            minimumAmounts
                                                    .getOrDefault(
                                                            pocketType,
                                                            0L
                                                    );

                                    long remainingAmount =
                                            Math.max(
                                                    budget.getTargetAmount()
                                                            - usedAmount,
                                                    0L
                                            );

                                    return PocketAdjustmentResponse
                                            .builder()
                                            .pocketId(
                                                    budget
                                                            .getPocket()
                                                            .getPocketId()
                                            )
                                            .pocketType(
                                                    pocketType
                                            )
                                            .pocketName(
                                                    budget
                                                            .getPocket()
                                                            .getPocketName()
                                            )
                                            .targetAmount(
                                                    budget
                                                            .getTargetAmount()
                                            )
                                            .usedAmount(
                                                    usedAmount
                                            )
                                            .remainingAmount(
                                                    remainingAmount
                                            )
                                            .build();
                                }
                        )
                        .toList();

        long totalUsedAmount =
                pockets
                        .stream()
                        .mapToLong(
                                PocketAdjustmentResponse
                                        ::getUsedAmount
                        )
                        .sum();

        return BudgetAdjustmentResponse
                .builder()
                .month(
                        YearMonth.from(
                                monthlyBudget
                                        .getBudgetMonth()
                        ).toString()
                )
                .totalBudgetAmount(
                        monthlyBudget
                                .getTotalBudgetAmount()
                )
                .totalUsedAmount(
                        totalUsedAmount
                )
                .pockets(
                        pockets
                )
                .build();
    }
}