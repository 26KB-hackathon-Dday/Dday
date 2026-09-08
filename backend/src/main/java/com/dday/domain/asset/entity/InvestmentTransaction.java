package com.dday.domain.asset.entity;

import com.dday.domain.mydata.entity.UserAccount;
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
 * 미래자산 포켓의 실적을 만드는 투자·적립 거래 한 건.
 *
 * <p>{@code financial_transaction}과 나눠둔 이유는 성격이 다르기 때문이다. 소비 거래는
 * "얼마 썼나" 하나면 되지만 투자는 수량·단가·상품코드가 있어야 평가액을 계산할 수 있다.
 * 한 테이블에 담으면 소비 거래 전부에 빈 컬럼이 대여섯 개 붙는다.
 *
 * <p>{@link #quantity}·{@link #unitPrice}는 적금 납입에는 없어서 nullable이다.
 * 소수점이 있는 값이라 {@link BigDecimal}을 쓴다 — 주식 소수점 매매가 있고,
 * {@code double}로 두면 수량 × 단가가 원 단위에서 어긋난다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "investment_transaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_investment_transaction_source",
                columnNames = {"account_id", "source_transaction_id"}
        ),
        indexes = @Index(name = "idx_investment_executed_at", columnList = "executed_at")
)
public class InvestmentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "investment_transaction_id")
    private Long investmentTransactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private UserAccount account;

    /** 마이데이터 원본 거래 ID. 계좌와 묶어 중복 수집을 막는다. */
    @Column(name = "source_transaction_id", nullable = false, length = 150)
    private String sourceTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private InvestmentActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 20)
    private ExecutionStatus executionStatus;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(name = "product_name", length = 150)
    private String productName;

    /** 거래 수량. 적금 납입에는 없다. */
    @Column(precision = 20, scale = 8)
    private BigDecimal quantity;

    /** 거래 단가. 적금 납입에는 없다. */
    @Column(name = "unit_price", precision = 20, scale = 4)
    private BigDecimal unitPrice;

    /**
     * 실제 오간 금액. 원 단위 정수다.
     *
     * <p>수량 × 단가로 다시 계산하지 않고 따로 저장한다. 수수료·세금 때문에 곱셈 결과와
     * 실제 금액이 다르고, 미래자산 실적에 필요한 건 실제로 나간 돈이다.
     */
    @Column(nullable = false)
    private Long amount;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private InvestmentTransaction(UserAccount account, String sourceTransactionId,
                                  AssetType assetType, InvestmentActionType actionType,
                                  String productCode, String productName, BigDecimal quantity,
                                  BigDecimal unitPrice, Long amount, LocalDateTime executedAt,
                                  LocalDateTime syncedAt) {
        this.account = account;
        this.sourceTransactionId = sourceTransactionId;
        this.assetType = assetType;
        this.actionType = actionType;
        this.productCode = productCode;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
        this.executedAt = executedAt;
        this.syncedAt = syncedAt;
        this.executionStatus = ExecutionStatus.EXECUTED;
    }

    /** 금융기관이 취소·정정한 거래. 행은 남기고 상태만 바꾼다. */
    public void markStatus(ExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }

    /** 자산이 늘어나는 방향인지. 실적 집계에서 매수·납입만 더한다. */
    public boolean isInflow() {
        return this.actionType == InvestmentActionType.BUY
                || this.actionType == InvestmentActionType.DEPOSIT;
    }
}
