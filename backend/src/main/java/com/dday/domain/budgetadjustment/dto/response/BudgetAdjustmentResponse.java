package com.dday.domain.budgetadjustment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class BudgetAdjustmentResponse {

    private Long monthlyBudgetId;

    private LocalDate budgetMonth;

    private Long totalBudgetAmount;

    /**
     * 네 포켓의 현재 사용액 합계.
     *
     * 총 예산을 직접 줄일 수 있게 할 경우
     * 총 예산의 최소값으로 사용할 수 있다.
     */
    private Long minimumTotalBudget;

    private List<PocketAdjustmentResponse> pockets;
}