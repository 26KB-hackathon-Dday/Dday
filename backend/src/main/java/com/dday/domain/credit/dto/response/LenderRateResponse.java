package com.dday.domain.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 한 업권의 예상 금리와, 목표 점수까지 올렸을 때의 금리·절약액.
 *
 * <p>화면의 Before / After / 연 절약 한 줄에 그대로 대응한다.
 *
 * <p><b>{@code null}과 0을 구별해야 한다.</b> {@code currentRate}가 {@code null}이면 그 점수대에
 * 공시가 없다는 뜻이고(금리 0%가 아니다), {@code annualSaving}이 0이면 목표 점수까지 올려도
 * 그 업권은 구간이 안 바뀐다는 뜻이다.
 */
@Getter
@Builder
@AllArgsConstructor
public class LenderRateResponse {

    private final LenderType lenderType;

    /** 화면에 그대로 쓰는 업권 이름 ("은행"·"캐피탈"·"카드사"). */
    private final String label;

    /** 이 평균에 들어간 회사 수. 공시가 없는 구간이면 {@code null}. */
    private final Integer institutionCount;

    /** 현재 점수의 업권 평균 금리(%). 공시가 없으면 {@code null}. */
    private final BigDecimal currentRate;

    /** 원금 기준 연 이자(원). */
    private final Long currentAnnualInterest;

    /** 목표 점수의 업권 평균 금리(%). 목표가 없거나 공시가 없으면 {@code null}. */
    private final BigDecimal targetRate;

    private final Long targetAnnualInterest;

    /** 연 절약액(원) = 현재 연 이자 − 목표 연 이자. 한쪽이라도 없으면 {@code null}. */
    private final Long annualSaving;
}
