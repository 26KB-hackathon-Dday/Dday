package com.dday.domain.mockmydata.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mock 금융기관이 들고 있는 계좌.
 *
 * <p>{@link #externalAccountId}가 Mock API 응답에 실려 나가는 <b>공개 식별자</b>다. 내부 PK를
 * 그대로 노출하지 않는 이유는 실제 마이데이터도 기관 발급 ID를 주기 때문이다 —
 * 여기서 PK를 노출해버리면 서비스 쪽이 "외부 ID는 숫자"라고 가정하게 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "mock_mydata_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mock_external_account",
                        columnNames = "external_account_id"
                ),
                @UniqueConstraint(
                        name = "uk_mock_user_account",
                        columnNames = {"mock_user_id", "org_code", "account_num"}
                )
        },
        indexes = @Index(name = "idx_mock_account_user", columnList = "mock_user_id")
)
public class MockMydataAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mock_account_id")
    private Long mockAccountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mock_user_id", nullable = false)
    private MockMydataUser mockUser;

    /** Mock API가 밖으로 내보내는 계좌 ID. */
    @Column(name = "external_account_id", nullable = false, length = 100)
    private String externalAccountId;

    @Column(name = "org_code", nullable = false, length = 20)
    private String orgCode;

    @Column(name = "account_num", nullable = false, length = 100)
    private String accountNum;

    @Column(name = "account_name", length = 100)
    private String accountName;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private MockAccountType accountType;

    @Column(nullable = false)
    private Long balance;

    /**
     * 연 이자율(%). 대출 계좌만 값이 있고 예금·적금은 {@code null}이다.
     *
     * <p>이율이라 {@link BigDecimal}이다 — 5.4%를 {@code double}로 두면 표시할 때
     * 5.3999…가 튀어나온다 (AGENTS.md §4).
     */
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "available_balance")
    private Long availableBalance;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private MockMydataAccount(MockMydataUser mockUser, String externalAccountId, String orgCode,
                              String accountNum, String accountName, String productName,
                              MockAccountType accountType, Long balance, Long availableBalance,
                              BigDecimal interestRate) {
        this.mockUser = mockUser;
        this.externalAccountId = externalAccountId;
        this.orgCode = orgCode;
        this.accountNum = accountNum;
        this.accountName = accountName;
        this.productName = productName;
        this.accountType = accountType;
        this.balance = balance != null ? balance : 0L;
        this.availableBalance = availableBalance;
        this.interestRate = interestRate;
        this.active = true;
    }

    /** 시연용 잔액 조정. Mock이라 거래 없이도 잔액을 바꿀 수 있어야 한다. */
    public void changeBalance(Long balance, Long availableBalance) {
        this.balance = balance;
        this.availableBalance = availableBalance;
    }

    public void deactivate() {
        this.active = false;
    }
}
