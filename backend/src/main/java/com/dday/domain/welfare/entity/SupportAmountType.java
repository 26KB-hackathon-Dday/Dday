package com.dday.domain.welfare.entity;

/**
 * 지원 금액의 성격. {@code benefitText}(화면 문구)와 포켓 반영 시뮬레이션(SUBSIDY-006, 후속)의
 * 월 환산 계산이 이 값으로 갈린다.
 */
public enum SupportAmountType {

    /** 총액 1회 지급 (예: 자립정착금 1,000만원). */
    FIXED,

    /** 매월 지급 (예: 청년월세지원 월 20만원). */
    MONTHLY,

    /** 한도액 — 실제 지급은 그 이하 (예: 학자금 대출 한도). */
    LIMIT,

    /** 반기 지급. */
    SEMIANNUAL
}
