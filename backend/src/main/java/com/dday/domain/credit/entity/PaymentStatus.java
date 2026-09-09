package com.dday.domain.credit.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 납부 결과.
 *
 * <p>{@code paidDate > dueDate}로 매번 계산하지 않고 판정 결과를 저장한다 — 며칠부터
 * 연체로 볼지는 기관마다 다르고 바뀔 수도 있어서, 판정 시점의 결론을 남기는 편이 안전하다.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    PAID("정상 납부"),
    LATE("연체"),
    UNPAID("미납");

    private final String label;
}
