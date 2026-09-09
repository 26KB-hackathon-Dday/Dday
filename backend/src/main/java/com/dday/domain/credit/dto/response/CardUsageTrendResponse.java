package com.dday.domain.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** 한 달의 카드 이용률. */
@Getter
@Builder
@AllArgsConstructor
public class CardUsageTrendResponse {

    /** "yyyy-MM". */
    private final String month;

    /** 그 달의 신용카드 사용액 합계(원). */
    private final Long usage;

    /** 한도 대비 이용률(%). */
    private final BigDecimal utilization;
}
