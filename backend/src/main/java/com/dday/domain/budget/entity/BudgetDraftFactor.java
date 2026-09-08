package com.dday.domain.budget.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 예산 초안이 그 금액이 된 이유 한 줄.
 *
 * <p>"이번 달 가용 예산 92만원"만 던지면 사용자는 믿을 근거가 없다. 급여 120만 - 월세 35만 -
 * 통신비 5만처럼 <b>더하고 뺀 항목을 그대로 보여주기 위해</b> 산출 과정을 행으로 남긴다.
 *
 * <p>수입인지 지출인지는 별도 컬럼이 아니라 {@link FactorType}이 들고 있다. 부호를 따로 두면
 * "지출인데 금액이 양수"인 행이 생겨 합계가 조용히 틀어진다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "budget_draft_factor",
        indexes = @Index(name = "idx_budget_draft_factor_budget", columnList = "monthly_budget_id")
)
public class BudgetDraftFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_draft_factor_id")
    private Long budgetDraftFactorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_budget_id", nullable = false)
    private MonthlyBudget monthlyBudget;

    @Enumerated(EnumType.STRING)
    @Column(name = "factor_type", nullable = false, length = 30)
    private FactorType factorType;

    /** 화면에 뜨는 항목 이름. 같은 유형이라도 "SKT 통신비"처럼 구체적으로 적는다. */
    @Column(name = "factor_name", nullable = false, length = 100)
    private String factorName;

    /** 항목 금액. 양수로 넣고, 수입인지 지출인지는 {@link #factorType}으로 판단한다. */
    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private FactorSourceType sourceType;

    /** "최근 3개월 평균" 같은 계산 근거. 사용자가 값을 의심할 때 펼쳐 보는 설명이다. */
    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private BudgetDraftFactor(MonthlyBudget monthlyBudget, FactorType factorType,
                              String factorName, Long amount, FactorSourceType sourceType,
                              String description) {
        this.monthlyBudget = monthlyBudget;
        this.factorType = factorType;
        this.factorName = factorName;
        this.amount = amount;
        this.sourceType = sourceType;
        this.description = description;
    }
}
