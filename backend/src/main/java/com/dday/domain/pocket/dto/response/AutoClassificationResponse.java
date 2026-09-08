package com.dday.domain.pocket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 자동 분류 실행 결과를 규칙 적용과 기본값 적용 건수로 나눠 보여준다. */
@Getter
@Builder
@AllArgsConstructor
public class AutoClassificationResponse {
    private int targetCount;
    private int userRuleCount;
    private int defaultFreeCount;
    private int remainingUnclassifiedCount;
}
