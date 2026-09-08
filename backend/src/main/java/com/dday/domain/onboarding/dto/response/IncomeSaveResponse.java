package com.dday.domain.onboarding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 추가한 수입의 id와, 추가 후 다시 계산한 합계. */
@Getter
@Builder
@AllArgsConstructor
public class IncomeSaveResponse {

    private final Long incomeId;
    private final long totalMonthly;

    public static IncomeSaveResponse of(Long incomeId, long totalMonthly) {
        return IncomeSaveResponse.builder().incomeId(incomeId).totalMonthly(totalMonthly).build();
    }
}
