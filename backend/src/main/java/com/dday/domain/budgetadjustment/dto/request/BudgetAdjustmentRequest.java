package com.dday.domain.budgetadjustment.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BudgetAdjustmentRequest {

    private Long totalBudgetAmount;

    private List<PocketAdjustmentRequest> allocations;
}