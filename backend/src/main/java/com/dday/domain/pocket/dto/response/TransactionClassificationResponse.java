package com.dday.domain.pocket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 수동 분류 결과와 향후 가맹점 규칙 생성 여부를 반환한다. */
@Getter
@Builder
@AllArgsConstructor
public class TransactionClassificationResponse {
    private Long transactionId;
    private ClassificationValueResponse previous;
    private ClassificationValueResponse current;
    private boolean futureRuleCreated;
    private LocalDateTime classifiedAt;
}
