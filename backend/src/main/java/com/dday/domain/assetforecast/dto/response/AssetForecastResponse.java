package com.dday.domain.assetforecast.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class AssetForecastResponse {

    /**
     * 현재 미래자산.
     *
     * 활성화된 SAVINGS + INVESTMENT 계좌의 잔액 합계.
     */
    private Long currentAsset;

    /**
     * 이번 달 미래자산 포켓 배정액.
     */
    private Long monthlyFutureAmount;

    /** 이번 달 적금·투자 계좌로 실제 이체한 금액. */
    private Long achievedAmount;

    /** 이번 달 배정액에서 달성액을 뺀 금액. 초과 달성 시 0이다. */
    private Long remainingAmount;

    /** achievedAmount / monthlyFutureAmount * 100. */
    private BigDecimal achievementRate;

    /**
     * 지원 종료까지 남은 개월 수.
     */
    private Long remainingMonths;

    /**
     * 지원 종료일.
     */
    private LocalDate supportEndDate;

    /**
     * 앞으로 추가로 쌓일 것으로 예상되는 금액.
     *
     * monthlyFutureAmount * remainingMonths
     */
    private Long expectedAdditionalAsset;

    /**
     * 지원 종료 시 예상 총자산.
     *
     * currentAsset + expectedAdditionalAsset
     */
    private Long expectedAsset;

    /**
     * 투자 수익률 반영 여부.
     *
     * 현재 계산에서는 수익률을 반영하지 않는다.
     */
    private boolean investmentReturnIncluded;

    private Long currentTotalAsset;
    private Long currentCashAsset;
    private Long currentSavingInvestmentAsset;
    private Long currentHousingDeposit;
    private Long forecastTotalAsset;
    private Long forecastNetAsset;
    private Long forecastShortageAmount;
    private Long forecastCashAsset;
    private Long forecastSavingInvestmentAsset;
    private Long forecastHousingDeposit;
    private Long monthlyConvertedNetIncome;
    private Long averageMonthlySavingInvestment;
    private Long totalAssetChange;
    private Long savingInvestmentChange;
    private Long cashDepletionMonth;
    private Long liquidAssetDepletionMonth;
    private Long housingDepositDepletionMonth;
    private BigDecimal cashRatio;
    private BigDecimal savingInvestmentRatio;
    private BigDecimal housingDepositRatio;
    private CalculationBasis calculationBasis;
    private boolean dataSufficient;

    @Getter
    @Builder
    public static class CalculationBasis {
        private List<Long> monthlyIncome;
        private List<Long> monthlyExpense;
        private Long currentFixedIncome;
        private List<Long> monthlySavingInvestment;
        private Integer validIncomeMonthCount;
    }
}
