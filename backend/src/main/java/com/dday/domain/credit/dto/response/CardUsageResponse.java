package com.dday.domain.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 카드 한도 대비 이용률 응답.
 *
 * <p><b>한도가 있는 카드만 담는다.</b> 체크·선불카드는 한도라는 개념이 없어 이용률을 낼 수
 * 없다 — 0원 한도가 아니라 계산 대상이 아니다.
 *
 * <p>연동한 신용카드가 없으면 404가 아니라 빈 응답을 준다.
 */
@Getter
@Builder
@AllArgsConstructor
public class CardUsageResponse {

    /** 추이에 담긴 개월 수. */
    private final int months;

    /** 기준이 되는 이번 달 "yyyy-MM". 신용카드가 없으면 {@code null}. */
    private final String currentMonth;

    /** 신용카드 한도 합계(원). */
    private final long totalCreditLimit;

    /** 이번 달 사용액 합계(원). */
    private final long currentUsage;

    /** 이번 달 이용률(%). 신용카드가 없으면 {@code null} — 0%와 구별해야 한다. */
    private final BigDecimal currentUtilization;

    /** 카드별 이번 달 현황. */
    private final List<CardUsageItemResponse> cards;

    /** 오래된 달부터. 거래가 없는 달도 0으로 채운다. */
    private final List<CardUsageTrendResponse> trend;

    /** 연동한 신용카드가 없는 회원. */
    public static CardUsageResponse empty() {
        return CardUsageResponse.builder()
                .cards(List.of())
                .trend(List.of())
                .build();
    }
}
