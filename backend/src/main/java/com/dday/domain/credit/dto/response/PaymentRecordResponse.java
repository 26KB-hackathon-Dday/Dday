package com.dday.domain.credit.dto.response;

import com.dday.domain.credit.entity.NonFinancialPayment;
import com.dday.domain.credit.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** 납부 이력 한 건. */
@Getter
@Builder
@AllArgsConstructor
public class PaymentRecordResponse {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 청구월 "yyyy-MM". 일자는 의미가 없어 잘라서 내려준다. */
    private final String billingMonth;

    private final Long amount;

    private final LocalDate dueDate;

    /** 실제 납부일. 미납이면 {@code null}. */
    private final LocalDate paidDate;

    private final PaymentStatus status;

    /** 화면에 그대로 쓰는 상태 문구. */
    private final String statusLabel;

    public static PaymentRecordResponse from(NonFinancialPayment payment) {
        return PaymentRecordResponse.builder()
                .billingMonth(payment.getBillingMonth().format(MONTH))
                .amount(payment.getAmount())
                .dueDate(payment.getDueDate())
                .paidDate(payment.getPaidDate())
                .status(payment.getStatus())
                .statusLabel(payment.getStatus().getLabel())
                .build();
    }
}
