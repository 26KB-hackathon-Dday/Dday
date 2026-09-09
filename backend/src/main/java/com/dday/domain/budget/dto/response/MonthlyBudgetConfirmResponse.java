package com.dday.domain.budget.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MonthlyBudgetConfirmResponse {
    private Long monthlyBudgetId;
    private String month;
    private Long totalBudgetAmount;
    private List<PocketBudgetResponse> pockets;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PocketBudgetResponse {
        private String pocketType;
        private Long targetAmount;
    }
}
