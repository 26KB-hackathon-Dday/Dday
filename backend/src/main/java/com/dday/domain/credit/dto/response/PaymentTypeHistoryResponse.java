package com.dday.domain.credit.dto.response;

import com.dday.domain.credit.entity.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 한 종류(통신요금·건강보험료·국민연금)의 납부 이력과 요약. */
@Getter
@Builder
@AllArgsConstructor
public class PaymentTypeHistoryResponse {

    private final PaymentType paymentType;

    /** 화면에 그대로 쓰는 종류 이름. */
    private final String label;

    /** 가장 최근 기록의 청구 기관명. */
    private final String institutionName;

    /** 가장 최근 청구월 "yyyy-MM". */
    private final String latestBillingMonth;

    /**
     * 최근부터 연속으로 제때 낸 개월 수.
     *
     * <p>연체·미납이 한 번 나오면 거기서 끊는다. "몇 번 냈나"가 아니라 "지금 얼마나 이어지고
     * 있나"가 가점 자료로 의미 있는 값이다.
     */
    private final int onTimeStreak;

    private final int lateCount;

    /** 최신 청구월부터. */
    private final List<PaymentRecordResponse> records;
}
