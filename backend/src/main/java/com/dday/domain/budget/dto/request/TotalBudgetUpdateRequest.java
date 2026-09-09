package com.dday.domain.budget.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TotalBudgetUpdateRequest {

    @NotNull(
            message = "총 예산은 필수입니다."
    )
    @Min(
            value = 1,
            message = "총 예산은 1원 이상이어야 합니다."
    )
    private Long totalBudgetAmount;
}