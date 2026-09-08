package com.dday.domain.onboarding.dto.response;

import com.dday.domain.income.entity.IncomeType;
import com.dday.domain.income.entity.RecurringIncome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 정기수입 한 건.
 *
 * <p>엔티티 필드명과 응답 필드명이 다르다({@code incomeName} → {@code name},
 * {@code expectedAmount} → {@code amount}, {@code depositTiming} → {@code paymentTiming}).
 * 화면 용어를 그대로 쓰려는 것이고, 변환은 여기 한 곳에서만 일어난다.
 */
@Getter
@Builder
@AllArgsConstructor
public class IncomeItemResponse {

    private final Long incomeId;
    private final String name;
    private final IncomeType incomeType;
    private final Long amount;
    private final String paymentTiming;

    public static IncomeItemResponse from(RecurringIncome income) {
        return IncomeItemResponse.builder()
                .incomeId(income.getRecurringIncomeId())
                .name(income.getIncomeName())
                .incomeType(income.getIncomeType())
                .amount(income.getExpectedAmount())
                .paymentTiming(income.getDepositTiming())
                .build();
    }
}
