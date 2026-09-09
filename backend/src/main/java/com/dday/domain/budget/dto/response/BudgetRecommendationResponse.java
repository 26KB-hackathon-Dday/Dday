package com.dday.domain.budget.dto.response;

import com.dday.domain.pocket.entity.PocketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class BudgetRecommendationResponse {

    /**
     * 이번 달 예상 총수입.
     *
     * 온보딩에서 등록한 recurring_income의 합계.
     */
    private Long totalBudgetAmount;

    /**
     * 온보딩 정기수입 합계.
     */
    private Long monthlyIncome;

    /**
     * 온보딩에서 입력한 월 주거비.
     *
     * 월세 + 관리비
     */
    private Long monthlyHousingCost;

    /**
     * 온보딩 시 입력한 현재 보유 자산.
     */
    private Long currentAsset;

    /**
     * 비상금 확보 목표.
     *
     * 3개월 주거비와 1개월 수입 중 큰 금액.
     */
    private Long emergencyReserveTarget;

    /**
     * 현재 자산이 비상금 목표보다 부족한지.
     */
    private boolean emergencyReserveNeeded;

    private List<PocketRecommendation> pockets;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PocketRecommendation {

        private PocketType pocketType;

        private Long amount;

        /**
         * 화면이나 Swagger에서
         * 왜 이 금액이 나왔는지 확인하기 위한 설명.
         */
        private String reason;
    }
}