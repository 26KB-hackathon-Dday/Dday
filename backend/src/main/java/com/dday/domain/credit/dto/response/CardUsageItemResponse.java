package com.dday.domain.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** 카드 한 장의 이번 달 이용 현황. */
@Getter
@Builder
@AllArgsConstructor
public class CardUsageItemResponse {

    private final Long cardId;

    private final String cardName;

    private final Long creditLimit;

    /** 이번 달 사용액(원). */
    private final Long usage;

    /** 한도 대비 이용률(%). */
    private final BigDecimal utilization;
}
