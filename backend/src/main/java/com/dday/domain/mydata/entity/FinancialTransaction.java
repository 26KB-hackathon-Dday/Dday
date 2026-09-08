package com.dday.domain.mydata.entity;

import com.dday.domain.pocket.entity.Category;
import com.dday.domain.pocket.entity.Pocket;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 마이데이터로 수집한 계좌·카드 거래 한 건. 포켓 화면이 읽는 소비 내역의 원본이다.
 *
 * <p><b>계좌 거래와 카드 거래를 한 테이블에 담는다.</b> 둘을 나누면 "이번 달 지출"을 뽑을 때마다
 * union을 해야 하고 분류 상태도 두 곳에 중복된다. 대신 {@link #sourceType}에 따라
 * {@link #account}와 {@link #card} 중 <b>정확히 하나만</b> 채워진다 — DDL의
 * {@code chk_financial_transaction_source}가 그 규칙을 DB에서 강제한다.
 *
 * <p><b>금액은 항상 양수로 저장한다.</b> 부호로 입출금을 나타내면 기관마다 부호 규칙이 달라
 * 수집할 때마다 어긋난다. 방향은 {@link #transactionType}이 들고 있다.
 *
 * <p>분류({@link #category}·{@link #pocket})는 이 테이블에 얹혀 있지만 Pocket 도메인의 관심사다.
 * 거래 원본은 MyData가 소유하고, 어느 포켓으로 셀지는 Pocket 도메인이 정한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "financial_transaction",
        uniqueConstraints = {
                // 같은 거래를 두 번 당겨도 행이 늘지 않게 하는 방어선. 계좌/카드 각각 건다.
                @UniqueConstraint(
                        name = "uk_account_source_transaction",
                        columnNames = {"account_id", "source_transaction_id"}
                ),
                @UniqueConstraint(
                        name = "uk_card_source_transaction",
                        columnNames = {"card_id", "source_transaction_id"}
                )
        },
        indexes = {
                @Index(name = "idx_financial_transaction_at", columnList = "transaction_at"),
                @Index(name = "idx_financial_transaction_pocket_at",
                        columnList = "pocket_id, transaction_at"),
                @Index(name = "idx_financial_transaction_classification",
                        columnList = "classification_status"),
                @Index(name = "idx_financial_transaction_account", columnList = "account_id"),
                @Index(name = "idx_financial_transaction_card", columnList = "card_id")
        }
)
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "financial_transaction_id")
    private Long financialTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private TransactionSourceType sourceType;

    /** 계좌 거래일 때만 채워진다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private UserAccount account;

    /** 카드 거래일 때만 채워진다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private UserCard card;

    /** 마이데이터 원본 거래 ID. 중복 수집을 막는 키라 반드시 원본 값 그대로 넣는다. */
    @Column(name = "source_transaction_id", nullable = false, length = 150)
    private String sourceTransactionId;

    /**
     * 본인 계좌 간 이체일 때 상대편 계좌. 이 값이 있으면
     * {@link TransactionType#SELF_TRANSFER}로 판정된 거래다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_account_id")
    private UserAccount counterpartyAccount;

    /** 취소·환불 거래가 가리키는 원 거래. 원 거래를 지우지 않고 이어붙인다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_transaction_id")
    private FinancialTransaction originalTransaction;

    /** 실제 거래가 일어난 시각. 집계 기준은 항상 이 값이지 수집 시각이 아니다. */
    @Column(name = "transaction_at", nullable = false)
    private LocalDateTime transactionAt;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false, length = 20)
    private TransactionStatus transactionStatus;

    /** 항상 양수. 입금인지 출금인지는 {@link #transactionType}이 말한다. */
    @Column(nullable = false)
    private Long amount;

    @Column(name = "merchant_name", length = 150)
    private String merchantName;

    /** 사업자등록번호. 있으면 가맹점 매칭이 이름보다 훨씬 정확해서 최우선으로 쓴다. */
    @Column(name = "merchant_regno", length = 30)
    private String merchantRegno;

    /** 거래 적요. 가맹점명이 없는 계좌 이체를 분류할 때 유일한 단서다. */
    @Column(name = "trans_memo", length = 300)
    private String transMemo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pocket_id")
    private Pocket pocket;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_status", nullable = false, length = 30)
    private ClassificationStatus classificationStatus;

    /** 아직 분류 전이면 {@code null}이다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "classification_source", length = 30)
    private ClassificationSource classificationSource;

    /**
     * 새로 들어온 돈을 사용자에게 알리고 확인받았는지. 입금이 잡히면 "예산에 반영할까요"를
     * 물어야 하는데, 이 플래그가 없으면 재동기화 때마다 같은 입금을 또 묻게 된다.
     */
    @Column(name = "new_fund_checked", nullable = false)
    private boolean newFundChecked;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private FinancialTransaction(TransactionSourceType sourceType, UserAccount account,
                                 UserCard card, String sourceTransactionId,
                                 UserAccount counterpartyAccount,
                                 FinancialTransaction originalTransaction,
                                 LocalDateTime transactionAt, LocalDateTime syncedAt,
                                 TransactionType transactionType, Long amount,
                                 String merchantName, String merchantRegno, String transMemo) {
        this.sourceType = sourceType;
        this.account = account;
        this.card = card;
        this.sourceTransactionId = sourceTransactionId;
        this.counterpartyAccount = counterpartyAccount;
        this.originalTransaction = originalTransaction;
        this.transactionAt = transactionAt;
        this.syncedAt = syncedAt;
        this.transactionType = transactionType;
        this.amount = amount;
        this.merchantName = merchantName;
        this.merchantRegno = merchantRegno;
        this.transMemo = transMemo;
        this.transactionStatus = TransactionStatus.NORMAL;
        this.classificationStatus = ClassificationStatus.UNCLASSIFIED;
        this.newFundChecked = true;
    }

    /**
     * 자동분류 결과를 붙인다. 사용자가 이미 손댄 거래는 건드리지 않는다 —
     * 재동기화 때 사용자의 수정이 조용히 되돌아가는 것을 막는다.
     */
    public void classifyAutomatically(Category category, Pocket pocket,
                                      ClassificationSource source) {
        if (this.classificationStatus == ClassificationStatus.MANUAL_CLASSIFIED) {
            return;
        }
        this.category = category;
        this.pocket = pocket;
        this.classificationSource = source;
        this.classificationStatus = ClassificationStatus.AUTO_CLASSIFIED;
    }

    /** 사용자가 직접 고친 분류. 이후 자동분류가 덮어쓰지 못한다. */
    public void classifyManually(Category category, Pocket pocket) {
        this.category = category;
        this.pocket = pocket;
        this.classificationSource = ClassificationSource.MANUAL;
        this.classificationStatus = ClassificationStatus.MANUAL_CLASSIFIED;
    }

    /** 취소·환불 반영. 행을 지우지 않고 상태만 바꾼다. */
    public void markStatus(TransactionStatus status, FinancialTransaction originalTransaction) {
        this.transactionStatus = status;
        this.originalTransaction = originalTransaction;
    }

    /** 본인 계좌 간 이체로 판정. 예산 집계에서 빠진다. */
    public void markAsSelfTransfer(UserAccount counterpartyAccount) {
        this.transactionType = TransactionType.SELF_TRANSFER;
        this.counterpartyAccount = counterpartyAccount;
    }

    public void markNewFundChecked() {
        this.newFundChecked = true;
    }

    /** 예산 소진액에 셀 거래인지. 취소·환불과 본인 이체는 제외한다. */
    public boolean countsTowardSpending() {
        return this.transactionStatus == TransactionStatus.NORMAL
                && this.transactionType == TransactionType.EXPENSE;
    }
}
