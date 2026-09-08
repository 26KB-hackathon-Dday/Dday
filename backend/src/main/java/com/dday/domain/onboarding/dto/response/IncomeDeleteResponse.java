package com.dday.domain.onboarding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 삭제 후 남은 수입의 합계. 프론트가 목록을 다시 부르지 않아도 되게 함께 준다. */
@Getter
@Builder
@AllArgsConstructor
public class IncomeDeleteResponse {

    private final long totalMonthly;

    public static IncomeDeleteResponse of(long totalMonthly) {
        return IncomeDeleteResponse.builder().totalMonthly(totalMonthly).build();
    }
}
