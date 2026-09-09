package com.dday.domain.credit.entity;

import com.dday.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 비금융 납부 이력 한 건 — 통신요금·건강보험료·국민연금을 그 달에 제때 냈는지.
 *
 * <p>{@code FinancialTransaction}과 나눠 두는 이유는 목적이 다르기 때문이다. 거래 내역은
 * "얼마 썼나"를 분류하는 데이터라 {@code dueDate}가 없다. 여기서는 <b>기한 대비 언제 냈는지</b>가
 * 전부라 납부기한이 없으면 데이터가 아무 뜻이 없다.
 *
 * <p>청구월은 그 달 1일로 저장한다. 월 단위 값이라 일자는 의미가 없지만, 문자열로 두면
 * 정렬·범위 조회가 사전순에 기대게 된다.
 *
 * <p>{@code (user_id, payment_type, billing_month)}에 유일 제약을 건다. 동기화를 여러 번
 * 돌려도 같은 달이 겹쳐 쌓이지 않게 하는 최종 방어선이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "nonfinancial_payment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_nonfinancial_payment_user_type_month",
                columnNames = {"user_id", "payment_type", "billing_month"}
        ),
        indexes = @Index(
                name = "idx_nonfinancial_payment_user_month",
                columnList = "user_id, billing_month"
        )
)
public class NonFinancialPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nonfinancial_payment_id")
    private Long nonfinancialPaymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentType paymentType;

    /** 청구 기관명 (SK텔레콤·국민건강보험공단·국민연금공단). 통신사는 회원마다 다르다. */
    @Column(name = "institution_name", nullable = false, length = 50)
    private String institutionName;

    /** 청구월. 그 달 1일로 저장한다. */
    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    /** 청구 금액(원). */
    @Column(nullable = false)
    private Long amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** 실제 납부일. 아직 안 냈으면 {@code null}이다. */
    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private NonFinancialPayment(User user, PaymentType paymentType, String institutionName,
                                LocalDate billingMonth, Long amount, LocalDate dueDate,
                                LocalDate paidDate, PaymentStatus status) {
        this.user = user;
        this.paymentType = paymentType;
        this.institutionName = institutionName;
        this.billingMonth = billingMonth;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paidDate = paidDate;
        this.status = status;
    }
}
