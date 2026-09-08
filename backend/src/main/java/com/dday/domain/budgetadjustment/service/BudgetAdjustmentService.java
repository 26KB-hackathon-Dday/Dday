package com.dday.domain.budgetadjustment.service;

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

    private final BudgetAdjustmentHistoryRepository historyRepository;

    private final BudgetAdjustmentDetailRepository detailRepository;

    /**
     * 진행 중인 이번 달 예산 조정 정보 조회.
     *
     * 확정된 월 예산만 대상으로 한다.
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
     * 진행 중인 이번 달 예산 재조정.
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

        Map<PocketType, Long> spentAmounts =
                calculateSpentAmounts(
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
                spentAmounts
        );

        long previousTotalBudget =
                monthlyBudget.getTotalBudgetAmount();

        long changedTotalBudget =
                request.getTotalBudgetAmount();

        BudgetChangeType changeType =
                previousTotalBudget == changedTotalBudget
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
                                        request.getChangeReason()
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
                    pocketBudget.getTargetAmount();

            Long changedAmount =
                    requestedAmounts.get(
                            pocketType
                    );

            /*
             * 금액이 실제로 바뀐 포켓만
             * 변경 상세에 기록한다.
             */
            if (!previousAmount.equals(changedAmount)) {

                detailRepository.save(
                        BudgetChangeDetail.builder()
                                .budgetChangeHistory(
                                        history
                                )
                                .pocket(
                                        pocketBudget.getPocket()
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
     * 이번 달 CONFIRMED 예산 조회.
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
     * 월 예산에 연결된 포켓별 예산 4개 조회.
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
     * 요청에 포켓 4개가 정확히 한 번씩 들어왔는지 검증.
     */
    private void validateRequestStructure(
            BudgetAdjustmentRequest request
    ) {
        if (
                request.getTotalBudgetAmount() == null
                        || request.getTotalBudgetAmount() <= 0
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .INVALID_TOTAL_BUDGET
            );
        }

        if (
                request.getPockets() == null
                        || request.getPockets().size() != 4
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
                    pocket.getPocketType() == null
                            || pocket.getAmount() == null
            ) {
                throw new BusinessException(
                        BudgetAdjustmentErrorCode
                                .INVALID_POCKET_COUNT
                );
            }

            if (
                    !types.add(
                            pocket.getPocketType()
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
     * 네 포켓 합이 총 예산과 정확히 일치해야 저장 가능.
     */
    private void validateBudgetSum(
            BudgetAdjustmentRequest request
    ) {
        long allocatedTotal =
                request.getPockets()
                        .stream()
                        .mapToLong(
                                BudgetAdjustmentRequest
                                        .PocketAmountRequest
                                        ::getAmount
                        )
                        .sum();

        if (
                allocatedTotal
                        != request.getTotalBudgetAmount()
        ) {
            throw new BusinessException(
                    BudgetAdjustmentErrorCode
                            .BUDGET_SUM_MISMATCH
            );
        }
    }

    /**
     * 이미 사용한 금액 아래로 예산을 내릴 수 없다.
     */
    private void validateMinimumAmounts(
            Map<PocketType, Long> requestedAmounts,
            Map<PocketType, Long> spentAmounts
    ) {
        for (
                PocketType pocketType
                : PocketType.values()
        ) {
            long requested =
                    requestedAmounts.getOrDefault(
                            pocketType,
                            0L
                    );

            long spent =
                    spentAmounts.getOrDefault(
                            pocketType,
                            0L
                    );

            if (requested < spent) {
                throw new BusinessException(
                        BudgetAdjustmentErrorCode
                                .BELOW_SPENT_AMOUNT
                );
            }
        }
    }

    private Map<PocketType, Long>
    createRequestedAmountMap(
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
                    pocket.getPocketType(),
                    pocket.getAmount()
            );
        }

        return result;
    }

    /**
     * 각 포켓의 이번 달 실제 사용액 집계.
     */
    private Map<PocketType, Long>
    calculateSpentAmounts(
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
                    pocketBudget
                            .getPocket()
                            .getPocketType(),
                    spent == null ? 0L : spent
            );
        }

        return result;
    }

    /**
     * 프론트에 내려줄 재조정 화면 데이터 생성.
     */
    private BudgetAdjustmentResponse createResponse(
            Long userId,
            MonthlyBudget monthlyBudget,
            List<MonthlyPocketBudget> pocketBudgets
    ) {
        LocalDate budgetMonth =
                monthlyBudget.getBudgetMonth();

        LocalDateTime from =
                budgetMonth.atStartOfDay();

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
                                    long spent =
                                            spentAmounts
                                                    .getOrDefault(
                                                            pocketBudget
                                                                    .getPocket()
                                                                    .getPocketType(),
                                                            0L
                                                    );

                                    long target =
                                            pocketBudget
                                                    .getTargetAmount();

                                    return PocketAdjustmentResponse
                                            .builder()
                                            .pocketId(
                                                    pocketBudget
                                                            .getPocket()
                                                            .getPocketId()
                                            )
                                            .pocketType(
                                                    pocketBudget
                                                            .getPocket()
                                                            .getPocketType()
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
                                                    spent
                                            )
                                            .remainingAmount(
                                                    Math.max(
                                                            target - spent,
                                                            0L
                                                    )
                                            )
                                            .build();
                                }
                        )
                        .toList();

        long minimumTotalBudget =
                pockets.stream()
                        .mapToLong(
                                PocketAdjustmentResponse
                                        ::getSpentAmount
                        )
                        .sum();

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