package com.dday.domain.budget.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TotalBudgetUpdateResponse {

    private LocalDate budgetMonth;

    private Long totalBudgetAmount;

    private Long minimumTotalBudget;
}