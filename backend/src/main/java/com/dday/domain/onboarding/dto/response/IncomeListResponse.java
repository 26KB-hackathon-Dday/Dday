package com.dday.domain.onboarding.dto.response;

import com.dday.domain.income.entity.RecurringIncome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class IncomeListResponse {

    private final List<IncomeItemResponse> items;

    /** 등록된 수입의 합계. 프론트가 더하지 않도록 서버가 계산해 내려준다. */
    private final long totalMonthly;

    public static IncomeListResponse of(List<RecurringIncome> incomes, long totalMonthly) {
        return IncomeListResponse.builder()
                .items(incomes.stream().map(IncomeItemResponse::from).toList())
                .totalMonthly(totalMonthly)
                .build();
    }
}
