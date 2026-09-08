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
 * Mock 계좌 거래 원본. 서비스가 동기화할 때 읽어가는 "외부 데이터"다.
 *
 * <p>{@link #originalTransactionId}는 FK가 아니라 <b>문자열</b>이다. 실제 마이데이터도 취소 거래를
 * 내려줄 때 원 거래의 내부 PK가 아니라 자기네 거래 ID를 준다 — 여기를 FK로 만들면
 * Mock이 실제보다 편해져서, 진짜 API를 붙였을 때 "문자열로 원 거래를 찾는" 코드가
 * 그제서야 필요해진다.
 *
 * <p>금액은 항상 양수다. 방향은 {@link #transactionType}이 들고 있다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "mock_mydata_account_transaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mock_account_transaction",
                columnNames = {"mock_account_id", "external_transaction_id"}
        ),
        indexes = {
                @Index(name = "idx_mock_account_transaction_at",
                        columnList = "mock_account_id, transaction_at"),
                @Index(name = "idx_mock_account_external_transaction",
                        columnList = "external_transaction_id")
        }
)
public class MockMydataAccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mock_transaction_id")
    private Long mockTransactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mock_account_id", nullable = false)
    private MockMydataAccount mockAccount;

    /** Mock API가 내려주는 거래 ID. 서비스는 이 값으로 중복 수집을 막는다. */
    @Column(name = "external_transaction_id", nullable = false, length = 150)
    private String externalTransactionId;

    @Column(name = "transaction_at", nullable = false)
    private LocalDateTime transactionAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private MockTransactionType transactionType;

    /** 항상 양수. */
    @Column(nullable = false)
    private Long amount;

    @Column(name = "counterparty_name", length = 100)
    private String counterpartyName;

    /** 상대 계좌번호. 서비스가 본인 계좌 간 이체인지 판정할 때 이 값을 본다. */
    @Column(name = "counterparty_account_num", length = 100)
    private String counterpartyAccountNum;

    @Column(name = "merchant_name", length = 150)
    private String merchantName;

    @Column(name = "merchant_regno", length = 30)
    private String merchantRegno;

    @Column(name = "trans_memo", length = 300)
    private String transMemo;

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
    private MockMydataAccountTransaction(MockMydataAccount mockAccount,
                                         String externalTransactionId,
                                         LocalDateTime transactionAt,
                                         MockTransactionType transactionType, Long amount,
                                         String counterpartyName, String counterpartyAccountNum,
                                         String merchantName, String merchantRegno,
                                         String transMemo, String originalTransactionId) {
        this.mockAccount = mockAccount;
        this.externalTransactionId = externalTransactionId;
        this.transactionAt = transactionAt;
        this.transactionType = transactionType;
        this.amount = amount;
        this.counterpartyName = counterpartyName;
        this.counterpartyAccountNum = counterpartyAccountNum;
        this.merchantName = merchantName;
        this.merchantRegno = merchantRegno;
        this.transMemo = transMemo;
        this.originalTransactionId = originalTransactionId;
        this.transactionStatus = MockTransactionStatus.NORMAL;
    }

    /** 시연용 취소·환불 처리. */
    public void markStatus(MockTransactionStatus status, String originalTransactionId) {
        this.transactionStatus = status;
        this.originalTransactionId = originalTransactionId;
    }
}
