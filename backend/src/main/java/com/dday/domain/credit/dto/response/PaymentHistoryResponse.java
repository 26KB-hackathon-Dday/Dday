package com.dday.domain.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 비금융 납부 이력 응답.
 *
 * <p>이력이 없는 회원은 404가 아니라 전부 0인 빈 응답을 받는다 — 아직 안 쌓인 것이지
 * 잘못된 요청이 아니다 (다른 신용 API와 같은 방식).
 */
@Getter
@Builder
@AllArgsConstructor
public class PaymentHistoryResponse {

    /** 이력이 있는 청구월 수. 종류가 여러 개여도 같은 달은 한 번만 센다. */
    private final int monthsCovered;

    private final int onTimeCount;

    private final int lateCount;

    private final int unpaidCount;

    /** 종류별. 통신요금 → 건강보험료 → 국민연금 순이며, 이력이 없는 종류는 빠진다. */
    private final List<PaymentTypeHistoryResponse> types;

    public static PaymentHistoryResponse empty() {
        return PaymentHistoryResponse.builder().types(List.of()).build();
    }
}
