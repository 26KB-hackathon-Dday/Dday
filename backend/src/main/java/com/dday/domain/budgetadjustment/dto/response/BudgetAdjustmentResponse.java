package com.dday.domain.budgetadjustment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BudgetAdjustmentResponse {

    private String month;

    private Long totalBudgetAmount;

    private Long totalUsedAmount;

    private List<PocketAdjustmentResponse> pockets;
}