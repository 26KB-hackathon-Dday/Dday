package com.dday.domain.mockmydata.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Mock 카드 승인·취소·환불 원본.
 *
 * <p>계좌 거래와 달리 {@code transaction_type}이 없다. 카드 승인은 언제나 지출이라
 * 방향을 따로 표시할 게 없기 때문이다 — 환불은 유형이 아니라
 * {@link MockTransactionStatus#REFUNDED} 상태로 표현한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "mock_mydata_card_transaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mock_card_transaction",
                columnNames = {"mock_card_id", "external_transaction_id"}
        ),
        indexes = {
                @Index(name = "idx_mock_card_transaction_at",
                        columnList = "mock_card_id, transaction_at"),
                @Index(name = "idx_mock_card_external_transaction",
                        columnList = "external_transaction_id")
        }
)
public class MockMydataCardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mock_card_transaction_id")
    private Long mockCardTransactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mock_card_id", nullable = false)
    private MockMydataCard mockCard;

    @Column(name = "external_transaction_id", nullable = false, length = 150)
    private String externalTransactionId;

    @Column(name = "transaction_at", nullable = false)
    private LocalDateTime transactionAt;

    /** 항상 양수. */
    @Column(nullable = false)
    private Long amount;

    @Column(name = "merchant_name", length = 150)
    private String merchantName;

    @Column(name = "merchant_regno", length = 30)
    private String merchantRegno;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false, length = 20)
    private MockTransactionStatus transactionStatus;

    /** 취소·환불 대상 거래의 {@code external_transaction_id}. FK가 아니다. */
    @Column(name = "original_transaction_id", length = 150)
    private String originalTransactionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private MockMydataCardTransaction(MockMydataCard mockCard, String externalTransactionId,
                                      LocalDateTime transactionAt, Long amount,
                                      String merchantName, String merchantRegno,
                                      String originalTransactionId) {
        this.mockCard = mockCard;
        this.externalTransactionId = externalTransactionId;
        this.transactionAt = transactionAt;
        this.amount = amount;
        this.merchantName = merchantName;
        this.merchantRegno = merchantRegno;
        this.originalTransactionId = originalTransactionId;
        this.transactionStatus = MockTransactionStatus.NORMAL;
    }

    public void markStatus(MockTransactionStatus status, String originalTransactionId) {
        this.transactionStatus = status;
        this.originalTransactionId = originalTransactionId;
    }
}
