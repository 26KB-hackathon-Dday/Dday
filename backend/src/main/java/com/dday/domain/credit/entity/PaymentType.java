package com.dday.domain.credit.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 비금융 납부 이력의 종류.
 *
 * <p>신용평가사가 가점 자료로 받는 항목들이다. 카드로 결제한 통신비
 * ({@code FinancialTransaction})와는 다른 데이터다 — 저쪽은 "얼마 썼나"이고
 * 이쪽은 "기한 안에 냈나"다.
 *
 * <p>{@link #label}은 화면에 그대로 쓰는 문구다. 정본을 서버에 둔다 (frontend/AGENTS.md).
 *
 * <p>선언 순서가 화면 표시 순서다.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentType {

    TELECOM("통신요금"),
    HEALTH_INSURANCE("건강보험료"),
    NATIONAL_PENSION("국민연금");

    private final String label;
}
