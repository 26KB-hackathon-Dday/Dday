package com.dday.domain.budgetadjustment.service;

import com.dday.domain.asset.entity.ExecutionStatus;
import com.dday.domain.asset.entity.InvestmentActionType;
import com.dday.domain.budget.dto.request.TotalBudgetUpdateRequest;
import com.dday.domain.budget.dto.response.TotalBudgetUpdateResponse;
import com.dday.domain.budget.entity.BudgetChangeDetail;
import com.dday.domain.budget.entity.BudgetChangeHistory;
import com.dday.domain.budget.entity.BudgetChangeType;
import com.dday.domain.budget.entity.BudgetStatus;
import com.dday.domain.budget.entity.MonthlyBudget;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.budgetadjustment.dto.BudgetAdjustmentErrorCode;
import com.dday.domain.budgetadjustment.dto.request.BudgetAdjustmentRequest;
import com.dday.domain.budgetadjustment.dto.response.BudgetAdjustmentResponse;
import com.dday.domain.budgetadjustment.dto.response.PocketAdjustmentResponse;
import com.dday.domain.budgetadjustment.repository.BudgetAdjustmentDetailRepository;
import com.dday.domain.budgetadjustment.repository.BudgetAdjustmentHistoryRepository;
import com.dday.domain.budgetadjustment.repository.BudgetAdjustmentInvestmentRepository;
import com.dday.domain.budgetadjustment.repository.BudgetAdjustmentMonthlyBudgetRepository;
import com.dday.domain.budgetadjustment.repository.BudgetAdjustmentMonthlyPocketBudgetRepository;
import com.dday.domain.budgetadjustment.repository.BudgetAdjustmentTransactionRepository;
import com.dday.domain.mydata.entity.TransactionStatus;
import com.dday.domain.mydata.entity.TransactionType;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BudgetAdjustmentService {

    private final BudgetAdjustmentMonthlyBudgetRepository monthlyBudgetRepository;

    private final BudgetAdjustmentMonthlyPocketBudgetRepository
            monthlyPocketBudgetRepository;

    private final BudgetAdjustmentTransactionRepository transactionRepository;

    private final BudgetAdjustmentInvestmentRepository investmentRepository;

    private final BudgetAdjustmentHistoryRepository historyRepository;

    private final BudgetAdjustmentDetailRepository detailRepository;

    /**
     * 이번 달 예산 재조정 정보 조회.
     */
    @Transactional(readOnly = true)
    public BudgetAdjustmentResponse findCurrent(
            Long userId
    ) {
        LocalDate budgetMonth =
                LocalDate.now()
                        .withDayOfMonth(1);

        MonthlyBudget monthlyBudget =
                findConfirmedBudget(
                        userId,
                        budgetMonth
                );

        List<MonthlyPocketBudget> pocketBudgets =
                findPocketBudgets(
                        monthlyBudget
                );

        return createResponse(
                userId,
                monthlyBudget,
                pocketBudgets
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
        LocalDate budgetMonth =
                LocalDate.now()
                        .withDayOfMonth(1);

        MonthlyBudget monthlyBudget =
                findConfirmedBudget(
                        userId,
                        budgetMonth
                );

        List<MonthlyPocketBudget> pocketBudgets =
                findPocketBudgets(
                        monthlyBudget
                );

        LocalDateTime from =
                budgetMonth.atStartOfDay();

        LocalDateTime to =
                budgetMonth
                        .plusMonths(1)
                        .atStartOfDay();

        Map<PocketType, Long> minimumAmounts =
                calculateMinimumAmounts(
                        userId,
                        pocketBudgets,
                        from,
                        to
                );

        long minimumTotalBudget =
                calculateMinimumTotalBudget(
                        minimumAmounts
                );

        long newTotalBudget =
                request.getTotalBudgetAmount();

        if (
                newTotalBudget
                        < minimumTotalBudget
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .TOTAL_BUDGET_BELOW_MINIMUM
            );
        }

        long previousTotalBudget =
                monthlyBudget
                        .getTotalBudgetAmount();

        /*
         * 실제로 금액이 달라질 때만
         * 변경 이력을 기록한다.
         */
        if (
                previousTotalBudget
                        != newTotalBudget
        ) {
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

            monthlyBudget.changeTotalAmount(
                    newTotalBudget
            );
        }

        return TotalBudgetUpdateResponse
                .builder()
                .budgetMonth(
                        monthlyBudget
                                .getBudgetMonth()
                )
                .totalBudgetAmount(
                        monthlyBudget
                                .getTotalBudgetAmount()
                )
                .minimumTotalBudget(
                        minimumTotalBudget
                )
                .build();
    }

    /**
     * 포켓 재배분 최종 저장.
     */
    @Transactional
    public BudgetAdjustmentResponse adjustCurrent(
            Long userId,
            BudgetAdjustmentRequest request
    ) {
        validateRequestStructure(
                request
        );

        LocalDate budgetMonth =
                LocalDate.now()
                        .withDayOfMonth(1);

        MonthlyBudget monthlyBudget =
                findConfirmedBudget(
                        userId,
                        budgetMonth
                );

        List<MonthlyPocketBudget> pocketBudgets =
                findPocketBudgets(
                        monthlyBudget
                );

        LocalDateTime from =
                budgetMonth.atStartOfDay();

        LocalDateTime to =
                budgetMonth
                        .plusMonths(1)
                        .atStartOfDay();

        Map<PocketType, Long> requestedAmounts =
                createRequestedAmountMap(
                        request
                );

        Map<PocketType, Long> minimumAmounts =
                calculateMinimumAmounts(
                        userId,
                        pocketBudgets,
                        from,
                        to
                );

        validateBudgetSum(
                request
        );

        validateMinimumAmounts(
                requestedAmounts,
                minimumAmounts
        );

        long previousTotalBudget =
                monthlyBudget
                        .getTotalBudgetAmount();

        long changedTotalBudget =
                request
                        .getTotalBudgetAmount();

        BudgetChangeType changeType =
                previousTotalBudget
                        == changedTotalBudget
                        ? BudgetChangeType.REALLOCATION
                        : BudgetChangeType.USER_EDIT;

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
                                        changedTotalBudget
                                )
                                .changeType(
                                        changeType
                                )
                                .changeReason(
                                        request
                                                .getChangeReason()
                                )
                                .build()
                );

        for (
                MonthlyPocketBudget pocketBudget
                : pocketBudgets
        ) {
            PocketType pocketType =
                    pocketBudget
                            .getPocket()
                            .getPocketType();

            Long previousAmount =
                    pocketBudget
                            .getTargetAmount();

            Long changedAmount =
                    requestedAmounts
                            .get(
                                    pocketType
                            );

            if (
                    !previousAmount
                            .equals(
                                    changedAmount
                            )
            ) {
                detailRepository.save(
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

            pocketBudget.changeTargetAmount(
                    changedAmount
            );
        }

        if (
                previousTotalBudget
                        != changedTotalBudget
        ) {
            monthlyBudget.changeTotalAmount(
                    changedTotalBudget
            );
        }

        return createResponse(
                userId,
                monthlyBudget,
                pocketBudgets
        );
    }

    /**
     * 현재 사용자의 이번 달 CONFIRMED 예산 조회.
     */
    private MonthlyBudget findConfirmedBudget(
            Long userId,
            LocalDate budgetMonth
    ) {
        return monthlyBudgetRepository
                .findByUserUserIdAndBudgetMonthAndBudgetStatus(
                        userId,
                        budgetMonth,
                        BudgetStatus.CONFIRMED
                )
                .orElseThrow(
                        () -> new BusinessException(
                                BudgetAdjustmentErrorCode
                                        .MONTHLY_BUDGET_NOT_FOUND
                        )
                );
    }

    /**
     * 월 예산에 연결된 포켓 4개 조회.
     */
    private List<MonthlyPocketBudget> findPocketBudgets(
            MonthlyBudget monthlyBudget
    ) {
        List<MonthlyPocketBudget> pocketBudgets =
                monthlyPocketBudgetRepository
                        .findAllByMonthlyBudgetId(
                                monthlyBudget
                                        .getMonthlyBudgetId()
                        );

        Set<PocketType> types =
                EnumSet.noneOf(
                        PocketType.class
                );

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
                !types.equals(
                        EnumSet.allOf(
                                PocketType.class
                        )
                )
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .POCKET_BUDGET_NOT_READY
            );
        }

        return pocketBudgets;
    }

    /**
     * 요청 구조 검증.
     */
    private void validateRequestStructure(
            BudgetAdjustmentRequest request
    ) {
        if (
                request
                        .getTotalBudgetAmount()
                        == null
                        ||
                        request
                                .getTotalBudgetAmount()
                                <= 0
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .INVALID_TOTAL_BUDGET
            );
        }

        if (
                request.getPockets()
                        == null
                        ||
                        request
                                .getPockets()
                                .size()
                                != 4
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .INVALID_POCKET_COUNT
            );
        }

        Set<PocketType> types =
                EnumSet.noneOf(
                        PocketType.class
                );

        for (
                BudgetAdjustmentRequest.PocketAmountRequest pocket
                : request.getPockets()
        ) {
            if (
                    pocket.getPocketType()
                            == null
                            ||
                            pocket.getAmount()
                                    == null
            ) {
                throw new BusinessException(
                        BudgetAdjustmentErrorCode
                                .INVALID_POCKET_COUNT
                );
            }

            if (
                    !types.add(
                            pocket
                                    .getPocketType()
                    )
            ) {
                throw new BusinessException(
                        BudgetAdjustmentErrorCode
                                .DUPLICATED_POCKET_TYPE
                );
            }
        }

        if (
                !types.equals(
                        EnumSet.allOf(
                                PocketType.class
                        )
                )
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .INVALID_POCKET_COUNT
            );
        }
    }

    /**
     * 네 포켓의 합이 총 예산과 같은지 검증.
     */
    private void validateBudgetSum(
            BudgetAdjustmentRequest request
    ) {
        long allocatedTotal =
                request
                        .getPockets()
                        .stream()
                        .mapToLong(
                                BudgetAdjustmentRequest
                                        .PocketAmountRequest
                                        ::getAmount
                        )
                        .sum();

        if (
                allocatedTotal
                        != request
                        .getTotalBudgetAmount()
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .BUDGET_SUM_MISMATCH
            );
        }
    }

    /**
     * 각 포켓의 재조정 최소 금액 검증.
     *
     * ESSENTIAL:
     * 실제 사용액
     *
     * FREE:
     * 실제 사용액
     *
     * FUTURE_ASSET:
     * 실제 투자/저축 달성액
     *
     * EMERGENCY:
     * 0
     */
    private void validateMinimumAmounts(
            Map<PocketType, Long> requestedAmounts,
            Map<PocketType, Long> minimumAmounts
    ) {
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

    private Map<PocketType, Long> createRequestedAmountMap(
            BudgetAdjustmentRequest request
    ) {
        Map<PocketType, Long> result =
                new EnumMap<>(
                        PocketType.class
                );

        for (
                BudgetAdjustmentRequest.PocketAmountRequest pocket
                : request.getPockets()
        ) {
            result.put(
                    pocket
                            .getPocketType(),
                    pocket
                            .getAmount()
            );
        }

        return result;
    }

    /**
     * 이번 달 소비 거래 집계.
     */
    private Map<PocketType, Long> calculateSpentAmounts(
            Long userId,
            List<MonthlyPocketBudget> pocketBudgets,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Map<PocketType, Long> result =
                new EnumMap<>(
                        PocketType.class
                );

        for (
                MonthlyPocketBudget pocketBudget
                : pocketBudgets
        ) {
            PocketType pocketType =
                    pocketBudget
                            .getPocket()
                            .getPocketType();

            /*
             * 미래자산과 비상금은
             * 소비 금액을 최소값으로 사용하지 않는다.
             */
            if (
                    pocketType
                            == PocketType.FUTURE_ASSET
                            ||
                            pocketType
                                    == PocketType.EMERGENCY
            ) {
                result.put(
                        pocketType,
                        0L
                );

                continue;
            }

            Long spent =
                    transactionRepository
                            .sumSpentAmount(
                                    userId,
                                    pocketBudget
                                            .getPocket()
                                            .getPocketId(),
                                    from,
                                    to,
                                    TransactionType.EXPENSE,
                                    TransactionStatus.NORMAL
                            );

            result.put(
                    pocketType,
                    spent == null
                            ? 0L
                            : spent
            );
        }

        return result;
    }

    /**
     * 미래자산의 이번 달 실제 달성액.
     *
     * BUY / DEPOSIT은 증가,
     * SELL / WITHDRAW는 감소로 계산한다.
     */
    private long calculateFutureAssetAchievedAmount(
            Long userId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Long increasedAmount =
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

        Long decreasedAmount =
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
                increasedAmount == null
                        ? 0L
                        : increasedAmount;

        long decreased =
                decreasedAmount == null
                        ? 0L
                        : decreasedAmount;

        return Math.max(
                increased - decreased,
                0L
        );
    }

    /**
     * 각 포켓의 최소 조정 가능 금액 계산.
     */
    private Map<PocketType, Long> calculateMinimumAmounts(
            Long userId,
            List<MonthlyPocketBudget> pocketBudgets,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Map<PocketType, Long> spentAmounts =
                calculateSpentAmounts(
                        userId,
                        pocketBudgets,
                        from,
                        to
                );

        long futureAchieved =
                calculateFutureAssetAchievedAmount(
                        userId,
                        from,
                        to
                );

        Map<PocketType, Long> result =
                new EnumMap<>(
                        PocketType.class
                );

        result.put(
                PocketType.ESSENTIAL,
                spentAmounts
                        .getOrDefault(
                                PocketType.ESSENTIAL,
                                0L
                        )
        );

        result.put(
                PocketType.FREE,
                spentAmounts
                        .getOrDefault(
                                PocketType.FREE,
                                0L
                        )
        );

        result.put(
                PocketType.FUTURE_ASSET,
                futureAchieved
        );

        /*
         * 비상금은 언제든 다른 포켓으로
         * 재배분 가능하므로 최소 0원.
         */
        result.put(
                PocketType.EMERGENCY,
                0L
        );

        return result;
    }

    /**
     * 설정 가능한 최소 총 예산.
     *
     * 비상금은 0원이므로 자동 제외된다.
     */
    private long calculateMinimumTotalBudget(
            Map<PocketType, Long> minimumAmounts
    ) {
        return minimumAmounts
                .values()
                .stream()
                .mapToLong(
                        Long::longValue
                )
                .sum();
    }

    /**
     * 프론트 재조정 화면 응답 생성.
     */
    private BudgetAdjustmentResponse createResponse(
            Long userId,
            MonthlyBudget monthlyBudget,
            List<MonthlyPocketBudget> pocketBudgets
    ) {
        LocalDate budgetMonth =
                monthlyBudget
                        .getBudgetMonth();

        LocalDateTime from =
                budgetMonth
                        .atStartOfDay();

        LocalDateTime to =
                budgetMonth
                        .plusMonths(1)
                        .atStartOfDay();

        Map<PocketType, Long> spentAmounts =
                calculateSpentAmounts(
                        userId,
                        pocketBudgets,
                        from,
                        to
                );

        Map<PocketType, Long> minimumAmounts =
                calculateMinimumAmounts(
                        userId,
                        pocketBudgets,
                        from,
                        to
                );

        List<PocketAdjustmentResponse> pockets =
                pocketBudgets
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        pocketBudget ->
                                                pocketBudget
                                                        .getPocket()
                                                        .getPocketType()
                                                        .ordinal()
                                )
                        )
                        .map(
                                pocketBudget -> {
                                    PocketType pocketType =
                                            pocketBudget
                                                    .getPocket()
                                                    .getPocketType();

                                    long target =
                                            pocketBudget
                                                    .getTargetAmount();

                                    long minimum =
                                            minimumAmounts
                                                    .getOrDefault(
                                                            pocketType,
                                                            0L
                                                    );

                                    long spent =
                                            spentAmounts
                                                    .getOrDefault(
                                                            pocketType,
                                                            0L
                                                    );

                                    /*
                                     * 화면에서는 spentAmount보다
                                     * minimumAmount를 잠금선 기준으로 사용한다.
                                     *
                                     * 미래자산은 minimumAmount가
                                     * 실제 달성액이다.
                                     */
                                    return PocketAdjustmentResponse
                                            .builder()
                                            .pocketId(
                                                    pocketBudget
                                                            .getPocket()
                                                            .getPocketId()
                                            )
                                            .pocketType(
                                                    pocketType
                                            )
                                            .pocketName(
                                                    pocketBudget
                                                            .getPocket()
                                                            .getPocketName()
                                            )
                                            .targetAmount(
                                                    target
                                            )
                                            .spentAmount(
                                                    spent
                                            )
                                            .minimumAmount(
                                                    minimum
                                            )
                                            .remainingAmount(
                                                    Math.max(
                                                            target - minimum,
                                                            0L
                                                    )
                                            )
                                            .build();
                                }
                        )
                        .toList();

        long minimumTotalBudget =
                calculateMinimumTotalBudget(
                        minimumAmounts
                );

        return BudgetAdjustmentResponse
                .builder()
                .monthlyBudgetId(
                        monthlyBudget
                                .getMonthlyBudgetId()
                )
                .budgetMonth(
                        monthlyBudget
                                .getBudgetMonth()
                )
                .totalBudgetAmount(
                        monthlyBudget
                                .getTotalBudgetAmount()
                )
                .minimumTotalBudget(
                        minimumTotalBudget
                )
                .pockets(
                        pockets
                )
                .build();
    }
}