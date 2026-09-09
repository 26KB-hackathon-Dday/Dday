package com.dday.domain.budget.service;

import com.dday.domain.budget.dto.BudgetErrorCode;
import com.dday.domain.budget.dto.request.MonthlyBudgetConfirmRequest;
import com.dday.domain.budget.dto.response.BudgetRecommendationResponse;
import com.dday.domain.budget.dto.response.MonthlyBudgetConfirmResponse;
import com.dday.domain.budget.entity.AllocationMethod;
import com.dday.domain.budget.entity.MonthlyBudget;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.budget.repository.MonthlyBudgetRepository;
import com.dday.domain.budget.repository.MonthlyPocketBudgetRepository;

import com.dday.domain.housing.entity.HousingCost;
import com.dday.domain.housing.repository.HousingCostRepository;

import com.dday.domain.income.entity.RecurringIncome;
import com.dday.domain.income.repository.RecurringIncomeRepository;

import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.repository.PocketRepository;

import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;

import com.dday.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    /*
     * ==================================================
     * 추천 예산 정책
     * ==================================================
     *
     * 생활방어 자금 부족
     *
     * 필수생활
     * → 비상금 20%
     * → 미래자산 10%
     * → 나머지 자유생활
     *
     *
     * 생활방어 자금 충분
     *
     * 필수생활
     * → 미래자산 20%
     * → 비상금 5%
     * → 나머지 자유생활
     */

    /**
     * 생활방어 자금이 부족할 때
     * 미래자산 권장 비율.
     */
    private static final double FUTURE_ASSET_RATE_DEFICIENT =
            0.10;

    /**
     * 생활방어 자금이 충분할 때
     * 미래자산 권장 비율.
     */
    private static final double FUTURE_ASSET_RATE_STABLE =
            0.20;

    /**
     * 생활방어 자금이 부족할 때
     * 비상금 권장 비율.
     */
    private static final double EMERGENCY_RATE_DEFICIENT =
            0.20;

    /**
     * 생활방어 자금이 충분할 때
     * 비상금 유지 비율.
     */
    private static final double EMERGENCY_RATE_STABLE =
            0.05;

    /**
     * 최소 생활방어 기준으로
     * 사용할 주거비 개월 수.
     */
    private static final int EMERGENCY_HOUSING_MONTHS =
            3;

    private final UserRepository userRepository;

    private final PocketRepository pocketRepository;

    private final MonthlyBudgetRepository monthlyBudgetRepository;

    private final MonthlyPocketBudgetRepository monthlyPocketBudgetRepository;

    private final RecurringIncomeRepository recurringIncomeRepository;

    private final HousingCostRepository housingCostRepository;

    /**
     * ==================================================
     * 온보딩 기반 이번 달 추천 예산
     * ==================================================
     *
     * 아직 실제 월 예산을 생성하지 않는다.
     *
     * 추천값을 화면에 보여주고,
     * 사용자가 그대로 확정하거나 수정한 뒤
     * confirmCurrent()에서 DB에 저장한다.
     */
    @Transactional(readOnly = true)
    public BudgetRecommendationResponse recommendCurrent(
            Long userId
    ) {

        User user =
                userRepository
                        .findByUserIdAndStatus(
                                userId,
                                UserStatus.ACTIVE
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        BudgetErrorCode.POCKETS_NOT_INITIALIZED
                                )
                        );

        /*
         * ==================================================
         * 1. 이번 달 예상 수입
         * ==================================================
         *
         * 온보딩에서 등록한 정기수입들의 합.
         */

        List<RecurringIncome> incomes =
                recurringIncomeRepository
                        .findAllByUserUserIdOrderByRecurringIncomeIdAsc(
                                userId
                        );

        long monthlyIncome =
                incomes
                        .stream()
                        .mapToLong(
                                income ->
                                        income.getExpectedAmount() == null
                                                ? 0L
                                                : income.getExpectedAmount()
                        )
                        .sum();

        long totalBudget =
                Math.max(
                        monthlyIncome,
                        0L
                );

        /*
         * ==================================================
         * 2. 월 필수 주거비
         * ==================================================
         *
         * 온보딩에서 입력한
         *
         * 월세
         * +
         * 관리비
         *
         * 를 사용한다.
         */

        long monthlyHousingCost =
                housingCostRepository
                        .findById(
                                userId
                        )
                        .map(
                                HousingCost::estimatedMonthly
                        )
                        .map(
                                value ->
                                        Math.max(
                                                value,
                                                0L
                                        )
                        )
                        .orElse(
                                0L
                        );

        /*
         * ==================================================
         * 3. 현재 모아둔 자산
         * ==================================================
         */

        long currentAsset =
                user.getInitialAsset() == null
                        ? 0L
                        : Math.max(
                        user.getInitialAsset(),
                        0L
                );

        /*
         * ==================================================
         * 4. 생활방어 목표 계산
         * ==================================================
         *
         * 생활방어 목표 =
         *
         * max(
         *   3개월치 주거비,
         *   1개월치 월수입
         * )
         */

        long threeMonthHousingCost =
                monthlyHousingCost
                        * EMERGENCY_HOUSING_MONTHS;

        long emergencyReserveTarget =
                Math.max(
                        threeMonthHousingCost,
                        monthlyIncome
                );

        boolean emergencyReserveNeeded =
                currentAsset
                        < emergencyReserveTarget;

        /*
         * ==================================================
         * 5. 필수 포켓
         * ==================================================
         *
         * 월세 + 관리비를 가장 먼저 확보.
         *
         * 단,
         * 월수입보다 주거비가 더 큰 경우
         * 총 예산까지만 배정한다.
         */

        long essential =
                Math.min(
                        totalBudget,
                        monthlyHousingCost
                );

        long remaining =
                totalBudget
                        - essential;

        long future;

        long emergency;

        /*
         * ==================================================
         * 6-A. 생활방어 자금 부족
         * ==================================================
         *
         * 현재 자산이 생활방어 목표보다 부족하면
         * 장기투자보다 비상금 확보를 우선한다.
         *
         * 비상금 20%
         * 미래자산 10%
         */

        if (emergencyReserveNeeded) {

            long emergencyTarget =
                    Math.round(
                            totalBudget
                                    * EMERGENCY_RATE_DEFICIENT
                    );

            /*
             * 부족한 경우에는
             * 비상금을 먼저 확보.
             */
            emergency =
                    Math.min(
                            remaining,
                            emergencyTarget
                    );

            remaining -=
                    emergency;

            long futureTarget =
                    Math.round(
                            totalBudget
                                    * FUTURE_ASSET_RATE_DEFICIENT
                    );

            future =
                    Math.min(
                            remaining,
                            futureTarget
                    );

            remaining -=
                    future;

        }

        /*
         * ==================================================
         * 6-B. 생활방어 자금 충분
         * ==================================================
         *
         * 생활방어 자금이 충분하면
         * 장기적인 자산형성을 더 우선한다.
         *
         * 미래자산 20%
         * 비상금 5%
         */

        else {

            long futureTarget =
                    Math.round(
                            totalBudget
                                    * FUTURE_ASSET_RATE_STABLE
                    );

            future =
                    Math.min(
                            remaining,
                            futureTarget
                    );

            remaining -=
                    future;

            long emergencyTarget =
                    Math.round(
                            totalBudget
                                    * EMERGENCY_RATE_STABLE
                    );

            emergency =
                    Math.min(
                            remaining,
                            emergencyTarget
                    );

            remaining -=
                    emergency;
        }

        /*
         * ==================================================
         * 7. 자유 포켓
         * ==================================================
         *
         * 필요한 항목을 먼저 확보하고
         * 실제로 남은 금액 전부.
         */

        long free =
                Math.max(
                        remaining,
                        0L
                );

        /*
         * ==================================================
         * 8. 응답
         * ==================================================
         */

        return BudgetRecommendationResponse
                .builder()
                .totalBudgetAmount(
                        totalBudget
                )
                .monthlyIncome(
                        monthlyIncome
                )
                .monthlyHousingCost(
                        monthlyHousingCost
                )
                .currentAsset(
                        currentAsset
                )
                .emergencyReserveTarget(
                        emergencyReserveTarget
                )
                .emergencyReserveNeeded(
                        emergencyReserveNeeded
                )
                .pockets(
                        List.of(

                                /*
                                 * 필수 포켓
                                 */
                                BudgetRecommendationResponse
                                        .PocketRecommendation
                                        .builder()
                                        .pocketType(
                                                PocketType.ESSENTIAL
                                        )
                                        .amount(
                                                essential
                                        )
                                        .reason(
                                                monthlyHousingCost > 0
                                                        ? "온보딩에서 입력한 월세와 관리비를 필수생활비로 먼저 확보했어요."
                                                        : "온보딩에 등록된 월 주거비가 없어 필수 포켓은 0원으로 시작해요."
                                        )
                                        .build(),

                                /*
                                 * 자유 포켓
                                 */
                                BudgetRecommendationResponse
                                        .PocketRecommendation
                                        .builder()
                                        .pocketType(
                                                PocketType.FREE
                                        )
                                        .amount(
                                                free
                                        )
                                        .reason(
                                                "필수생활비와 생활방어 자금, 미래자산을 확보한 뒤 남은 금액이에요."
                                        )
                                        .build(),

                                /*
                                 * 미래자산 포켓
                                 */
                                BudgetRecommendationResponse
                                        .PocketRecommendation
                                        .builder()
                                        .pocketType(
                                                PocketType.FUTURE_ASSET
                                        )
                                        .amount(
                                                future
                                        )
                                        .reason(
                                                emergencyReserveNeeded
                                                        ? "생활방어 자금이 아직 부족해 미래자산 비중을 월수입의 10%로 낮췄어요."
                                                        : "생활방어 자금이 충분해 장기 자산 형성을 위해 월수입의 20%를 배정했어요."
                                        )
                                        .build(),

                                /*
                                 * 비상금 포켓
                                 */
                                BudgetRecommendationResponse
                                        .PocketRecommendation
                                        .builder()
                                        .pocketType(
                                                PocketType.EMERGENCY
                                        )
                                        .amount(
                                                emergency
                                        )
                                        .reason(
                                                emergencyReserveNeeded
                                                        ? "현재 자산이 생활방어 목표보다 부족해 월수입의 20%를 비상금으로 우선 확보했어요."
                                                        : "현재 자산이 생활방어 목표 이상이라 월수입의 5%만 비상금 유지 금액으로 배정했어요."
                                        )
                                        .build()
                        )
                )
                .build();
    }

    /**
     * ==================================================
     * 이번 달 예산 확정
     * ==================================================
     */
    @Transactional
    public MonthlyBudgetConfirmResponse confirmCurrent(
            Long userId,
            MonthlyBudgetConfirmRequest request
    ) {

        LocalDate budgetMonth =
                LocalDate
                        .now()
                        .withDayOfMonth(
                                1
                        );

        /*
         * 이미 이번 달 예산이 존재하면
         * 최초 확정 불가.
         */
        if (
                monthlyBudgetRepository
                        .findByUserUserIdAndBudgetMonth(
                                userId,
                                budgetMonth
                        )
                        .isPresent()
        ) {

            throw new BusinessException(
                    BudgetErrorCode.MONTHLY_BUDGET_ALREADY_EXISTS
            );
        }

        var requestedTypes =
                new HashSet<PocketType>();

        long allocatedTotal =
                0L;

        /*
         * 네 포켓 중복 여부 및
         * 배정 금액 합계 계산.
         */
        for (
                var pocket
                : request.getPockets()
        ) {

            if (
                    !requestedTypes.add(
                            pocket.getPocketType()
                    )
            ) {

                throw new BusinessException(
                        BudgetErrorCode.INVALID_POCKET_BUDGETS
                );
            }

            allocatedTotal =
                    Math.addExact(
                            allocatedTotal,
                            pocket.getAmount()
                    );
        }

        /*
         * 네 포켓이 모두 존재해야 함.
         */
        if (
                requestedTypes.size()
                        != PocketType.values().length
        ) {

            throw new BusinessException(
                    BudgetErrorCode.INVALID_POCKET_BUDGETS
            );
        }

        /*
         * 네 포켓 합계는
         * 총 예산과 정확히 같아야 함.
         */
        if (
                allocatedTotal
                        != request.getTotalBudgetAmount()
        ) {

            throw new BusinessException(
                    BudgetErrorCode.BUDGET_TOTAL_MISMATCH
            );
        }

        User user =
                userRepository
                        .findByUserIdAndStatus(
                                userId,
                                UserStatus.ACTIVE
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        BudgetErrorCode.POCKETS_NOT_INITIALIZED
                                )
                        );

        List<Pocket> pockets =
                pocketRepository
                        .findAllByUserUserId(
                                userId
                        );

        if (
                pockets.size()
                        != PocketType.values().length
        ) {

            throw new BusinessException(
                    BudgetErrorCode.POCKETS_NOT_INITIALIZED
            );
        }

        var pocketByType =
                new EnumMap<PocketType, Pocket>(
                        PocketType.class
                );

        pockets.forEach(
                pocket ->
                        pocketByType.put(
                                pocket.getPocketType(),
                                pocket
                        )
        );

        /*
         * 월 예산 생성.
         */
        MonthlyBudget monthlyBudget =
                monthlyBudgetRepository.save(
                        MonthlyBudget
                                .builder()
                                .user(
                                        user
                                )
                                .budgetMonth(
                                        budgetMonth
                                )
                                .totalBudgetAmount(
                                        request.getTotalBudgetAmount()
                                )
                                .draftSummary(
                                        "온보딩 기반 개인화 추천 예산을 사용자가 확인 후 확정"
                                )
                                .draftCreatedAt(
                                        LocalDateTime.now()
                                )
                                .build()
                );

        monthlyBudget.confirm(
                LocalDateTime.now()
        );

        /*
         * 포켓별 월 예산 생성.
         */
        List<MonthlyPocketBudget> savedPockets =
                request
                        .getPockets()
                        .stream()
                        .map(
                                item ->
                                        MonthlyPocketBudget
                                                .builder()
                                                .monthlyBudget(
                                                        monthlyBudget
                                                )
                                                .pocket(
                                                        pocketByType.get(
                                                                item.getPocketType()
                                                        )
                                                )
                                                .targetAmount(
                                                        item.getAmount()
                                                )
                                                .allocationMethod(
                                                        AllocationMethod.USER_INPUT
                                                )
                                                .build()
                        )
                        .toList();

        monthlyPocketBudgetRepository.saveAll(
                savedPockets
        );

        /*
         * 응답.
         */
        return MonthlyBudgetConfirmResponse
                .builder()
                .monthlyBudgetId(
                        monthlyBudget.getMonthlyBudgetId()
                )
                .month(
                        budgetMonth
                                .toString()
                                .substring(
                                        0,
                                        7
                                )
                )
                .totalBudgetAmount(
                        monthlyBudget.getTotalBudgetAmount()
                )
                .pockets(
                        savedPockets
                                .stream()
                                .map(
                                        item ->
                                                MonthlyBudgetConfirmResponse
                                                        .PocketBudgetResponse
                                                        .builder()
                                                        .pocketType(
                                                                item
                                                                        .getPocket()
                                                                        .getPocketType()
                                                                        .name()
                                                        )
                                                        .targetAmount(
                                                                item.getTargetAmount()
                                                        )
                                                        .build()
                                )
                                .toList()
                )
                .build();
    }
}