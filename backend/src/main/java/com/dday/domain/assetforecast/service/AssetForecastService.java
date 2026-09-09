package com.dday.domain.assetforecast.service;

import com.dday.domain.assetforecast.dto.response.AssetForecastResponse;
import com.dday.domain.assetforecast.repository.AssetForecastAccountRepository;
import com.dday.domain.assetforecast.repository.AssetForecastPocketBudgetRepository;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.housing.repository.HousingCostRepository;
import com.dday.domain.income.repository.RecurringIncomeRepository;
import com.dday.domain.mydata.entity.AccountType;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.onboarding.service.OnboardingCalculator;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.user.dto.UserErrorCode;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import com.dday.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetForecastService {

    private final UserRepository
            userRepository;

    private final AssetForecastAccountRepository
            assetForecastAccountRepository;

    private final AssetForecastPocketBudgetRepository
            assetForecastPocketBudgetRepository;

    private final FinancialTransactionRepository
            financialTransactionRepository;

    private final HousingCostRepository housingCostRepository;
    private final RecurringIncomeRepository recurringIncomeRepository;

    /**
     * 지원 종료 시 예상 총자산 조회.
     *
     * 현재 미래자산
     * = SAVINGS + INVESTMENT 계좌 잔액
     *
     * 앞으로 쌓일 예상 자산
     * = 현재 월 FUTURE_ASSET 포켓 배정액
     *   × 지원 종료까지 남은 개월 수
     *
     * 예상 총자산
     * = 현재 미래자산
     *   + 앞으로 쌓일 예상 자산
     *
     * 투자 수익률은 반영하지 않는다.
     */
    public AssetForecastResponse getForecast(
            Long userId
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                UserErrorCode.USER_NOT_FOUND
                                        )
                        );

        LocalDate today =
                LocalDate.now();

        LocalDate protectionEndDate =
                user.getProtectionEndDate();

        /*
         * 보호종료일이 없으면
         * 지원 종료일과 남은 개월 수를
         * 계산할 수 없다.
         */
        if (protectionEndDate == null) {

            throw new BusinessException(
                    CommonErrorCode.INVALID_INPUT_VALUE
            );
        }

        LocalDate supportEndDate =
                OnboardingCalculator
                        .supportEndDate(
                                protectionEndDate
                        );

        long calculatedRemainingMonths =
                OnboardingCalculator
                        .remainingMonths(
                                protectionEndDate,
                                today
                        );

        /*
         * 이미 지원 기간이 끝난 경우
         * 앞으로 적립 가능한 개월 수는 0으로 본다.
         */
        long remainingMonths =
                Math.max(
                        calculatedRemainingMonths,
                        0L
                );

        /*
         * 현재 미래자산:
         *
         * 활성 SAVINGS +
         * 활성 INVESTMENT 계좌 잔액.
         */
        Long currentAsset =
                assetForecastAccountRepository
                        .sumCurrentFutureAsset(
                                userId,
                                List.of(
                                        AccountType.SAVINGS,
                                        AccountType.INVESTMENT
                                )
                        );

        if (currentAsset == null) {
            currentAsset = 0L;
        }

        /*
         * 이번 달 FUTURE_ASSET
         * 포켓 배정액 조회.
         */
        LocalDate budgetMonth =
                today.withDayOfMonth(1);

        Long monthlyFutureAmount =
                assetForecastPocketBudgetRepository
                        .findByUserAndMonthAndPocketType(
                                userId,
                                budgetMonth,
                                PocketType.FUTURE_ASSET
                        )
                        .map(
                                MonthlyPocketBudget::getTargetAmount
                        )
                        .orElse(0L);

        LocalDateTime monthStart = budgetMonth.atStartOfDay();
        LocalDateTime nextMonthStart = budgetMonth.plusMonths(1).atStartOfDay();
        Long achievedAmount = financialTransactionRepository
                .sumMonthlyFutureAssetContribution(userId, monthStart, nextMonthStart);
        if (achievedAmount == null) {
            achievedAmount = 0L;
        }
        long remainingAmount = Math.max(monthlyFutureAmount - achievedAmount, 0L);
        BigDecimal achievementRate = monthlyFutureAmount == 0L
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(achievedAmount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(monthlyFutureAmount), 1, RoundingMode.HALF_UP);

        long currentCash = assetForecastAccountRepository
                .sumActiveBalanceByType(userId, AccountType.DEPOSIT);
        long housingDeposit = housingCostRepository.findById(userId)
                .map(cost -> cost.getDeposit() == null ? 0L : cost.getDeposit())
                .orElse(0L);
        long currentFixedIncome = recurringIncomeRepository.sumExpectedAmountByUserId(userId);

        List<Long> monthlyIncome = new ArrayList<>();
        List<Long> monthlyExpense = new ArrayList<>();
        List<Long> monthlyContribution = new ArrayList<>();
        BigDecimal convertedNetSum = BigDecimal.ZERO;
        int validIncomeMonths = 0;

        LocalDate historyStart = budgetMonth.minusMonths(3);
        for (int index = 0; index < 3; index++) {
            LocalDate month = historyStart.plusMonths(index);
            LocalDateTime from = month.atStartOfDay();
            LocalDateTime to = month.plusMonths(1).atStartOfDay();
            long income = financialTransactionRepository.sumIncomeByPeriod(userId, from, to);
            long expense = financialTransactionRepository.sumExpenseByPeriod(userId, from, to);
            long contribution = financialTransactionRepository
                    .sumMonthlyFutureAssetContribution(userId, from, to);
            monthlyIncome.add(income);
            monthlyExpense.add(expense);
            monthlyContribution.add(contribution);
            if (income > 0L) {
                BigDecimal adjustedExpense = BigDecimal.valueOf(expense)
                        .multiply(BigDecimal.valueOf(currentFixedIncome))
                        .divide(BigDecimal.valueOf(income), 8, RoundingMode.HALF_UP);
                convertedNetSum = convertedNetSum.add(
                        BigDecimal.valueOf(currentFixedIncome).subtract(adjustedExpense));
                validIncomeMonths++;
            }
        }

        long convertedMonthlyNet = validIncomeMonths == 0 ? 0L : convertedNetSum
                .divide(BigDecimal.valueOf(validIncomeMonths), 0, RoundingMode.HALF_UP)
                .longValue();
        long averageMonthlyContribution = BigDecimal.valueOf(
                        monthlyContribution.stream().mapToLong(Long::longValue).sum())
                .divide(BigDecimal.valueOf(3), 0, RoundingMode.HALF_UP)
                .longValue();

        long forecastCash = currentCash;
        long forecastSavingInvestment = currentAsset;
        long forecastHousingDeposit = housingDeposit;
        Long cashDepletionMonth = null;
        Long liquidAssetDepletionMonth = null;
        Long housingDepositDepletionMonth = null;
        long forecastShortageAmount = 0L;
        for (long month = 1; month <= remainingMonths; month++) {
            forecastSavingInvestment += averageMonthlyContribution;
            forecastCash += convertedMonthlyNet;
            if (forecastCash <= 0L) {
                long shortfall = Math.abs(Math.min(forecastCash, 0L));
                forecastCash = 0L;
                if (cashDepletionMonth == null && convertedMonthlyNet < 0L) {
                    cashDepletionMonth = month;
                }
                long savingDeduction = Math.min(forecastSavingInvestment, shortfall);
                forecastSavingInvestment -= savingDeduction;
                shortfall -= savingDeduction;
                if (forecastSavingInvestment == 0L && liquidAssetDepletionMonth == null
                        && convertedMonthlyNet < 0L) {
                    liquidAssetDepletionMonth = month;
                }
                if (shortfall > 0L) {
                    long housingDeduction = Math.min(forecastHousingDeposit, shortfall);
                    forecastHousingDeposit -= housingDeduction;
                    shortfall -= housingDeduction;
                    if (forecastHousingDeposit == 0L && housingDepositDepletionMonth == null) {
                        housingDepositDepletionMonth = month;
                    }
                    forecastShortageAmount += shortfall;
                }
            }
        }

        long currentTotalAsset = currentCash + currentAsset + housingDeposit;
        long forecastTotalAsset = forecastCash + forecastSavingInvestment + forecastHousingDeposit;
        long forecastNetAsset = forecastTotalAsset - forecastShortageAmount;
        long expectedAdditionalAsset = forecastSavingInvestment - currentAsset;
        long expectedAsset = forecastNetAsset;
        BigDecimal totalForRatio = BigDecimal.valueOf(forecastTotalAsset);
        BigDecimal cashRatio = ratio(forecastCash, totalForRatio);
        BigDecimal savingRatio = ratio(forecastSavingInvestment, totalForRatio);
        BigDecimal housingRatio = ratio(forecastHousingDeposit, totalForRatio);

        return AssetForecastResponse
                .builder()
                .currentAsset(
                        currentAsset
                )
                .monthlyFutureAmount(
                        monthlyFutureAmount
                )
                .achievedAmount(
                        achievedAmount
                )
                .remainingAmount(
                        remainingAmount
                )
                .achievementRate(
                        achievementRate
                )
                .remainingMonths(
                        remainingMonths
                )
                .supportEndDate(
                        supportEndDate
                )
                .expectedAdditionalAsset(
                        expectedAdditionalAsset
                )
                .expectedAsset(
                        expectedAsset
                )
                .investmentReturnIncluded(
                        false
                )
                .currentTotalAsset(currentTotalAsset)
                .currentCashAsset(currentCash)
                .currentSavingInvestmentAsset(currentAsset)
                .currentHousingDeposit(housingDeposit)
                .forecastTotalAsset(forecastTotalAsset)
                .forecastNetAsset(forecastNetAsset)
                .forecastShortageAmount(forecastShortageAmount)
                .forecastCashAsset(forecastCash)
                .forecastSavingInvestmentAsset(forecastSavingInvestment)
                .forecastHousingDeposit(forecastHousingDeposit)
                .monthlyConvertedNetIncome(convertedMonthlyNet)
                .averageMonthlySavingInvestment(averageMonthlyContribution)
                .totalAssetChange(forecastNetAsset - currentTotalAsset)
                .savingInvestmentChange(forecastSavingInvestment - currentAsset)
                .cashDepletionMonth(cashDepletionMonth)
                .liquidAssetDepletionMonth(liquidAssetDepletionMonth)
                .housingDepositDepletionMonth(housingDepositDepletionMonth)
                .cashRatio(cashRatio)
                .savingInvestmentRatio(savingRatio)
                .housingDepositRatio(housingRatio)
                .calculationBasis(AssetForecastResponse.CalculationBasis.builder()
                        .monthlyIncome(monthlyIncome)
                        .monthlyExpense(monthlyExpense)
                        .currentFixedIncome(currentFixedIncome)
                        .monthlySavingInvestment(monthlyContribution)
                        .validIncomeMonthCount(validIncomeMonths)
                        .build())
                .dataSufficient(currentFixedIncome > 0L && validIncomeMonths > 0)
                .build();
    }

    private BigDecimal ratio(long amount, BigDecimal total) {
        if (total.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(100))
                .divide(total, 1, RoundingMode.HALF_UP);
    }
}
