package com.dday.domain.pocket.entity;

import com.dday.domain.mydata.entity.FinancialTransaction;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 거래 하나의 포켓·카테고리가 바뀐 기록.
 *
 * <p>분류가 왜 이렇게 됐는지를 사후에 설명할 수 있어야 해서 남긴다. "내가 바꾼 적 없는데
 * 카테고리가 달라졌다"는 문의가 들어오면 여기만 보면 자동분류가 건드린 건지
 * 사용자가 바꾼 건지 알 수 있다.
 *
 * <p>변경 전후가 모두 nullable이다 — 미분류 거래에 처음 분류가 붙는 것도 변경이고,
 * 그때 '변경 전'은 없기 때문이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "transaction_classification_history",
        indexes = @Index(
                name = "idx_classification_history_transaction",
                columnList = "financial_transaction_id, changed_at"
        )
)
public class TransactionClassificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "classification_history_id")
    private Long classificationHistoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_transaction_id", nullable = false)
    private FinancialTransaction financialTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_category_id")
    private Category previousCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_category_id")
    private Category changedCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_pocket_id")
    private Pocket previousPocket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_pocket_id")
    private Pocket changedPocket;

    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by", nullable = false, length = 20)
    private ClassificationChangedBy changedBy;

    /**
     * 사용자가 "앞으로 같은 가맹점도 이렇게"를 골랐는지. 이 값이 {@code true}인 변경은
     * {@link UserMerchantRule} 한 줄로도 이어진다 — 여기 남겨두면 규칙이 어느 변경에서
     * 생겼는지 되짚을 수 있다.
     */
    @Column(name = "apply_future_rule", nullable = false)
    private boolean applyFutureRule;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Builder
    private TransactionClassificationHistory(FinancialTransaction financialTransaction,
                                             Category previousCategory, Category changedCategory,
                                             Pocket previousPocket, Pocket changedPocket,
                                             ClassificationChangedBy changedBy,
                                             boolean applyFutureRule) {
        this.financialTransaction = financialTransaction;
        this.previousCategory = previousCategory;
        this.changedCategory = changedCategory;
        this.previousPocket = previousPocket;
        this.changedPocket = changedPocket;
        this.changedBy = changedBy;
        this.applyFutureRule = applyFutureRule;
    }
}
