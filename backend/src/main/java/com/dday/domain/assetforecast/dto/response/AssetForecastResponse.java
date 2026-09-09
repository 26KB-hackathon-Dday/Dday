package com.dday.domain.assetforecast.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

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
}