package com.dday.domain.budget.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 월 예산이 한 번 바뀐 사건.
 *
 * <p>총액의 전후만 여기 두고, 어느 포켓이 얼마에서 얼마로 갔는지는
 * {@link BudgetChangeDetail}에 여러 줄로 붙는다. 포켓 사이 재배분은 총액이 그대로라
 * ({@link BudgetChangeType#REALLOCATION}) 이 행만 봐서는 무엇이 바뀌었는지 알 수 없다 —
 * 상세가 있어야 완결된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "budget_change_history",
        indexes = @Index(
                name = "idx_budget_change_history_budget",
                columnList = "monthly_budget_id, changed_at"
        )
)
public class BudgetChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_change_history_id")
    private Long budgetChangeHistoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_budget_id", nullable = false)
    private MonthlyBudget monthlyBudget;

    @Column(name = "previous_total_budget", nullable = false)
    private Long previousTotalBudget;

    @Column(name = "changed_total_budget", nullable = false)
    private Long changedTotalBudget;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private BudgetChangeType changeType;

    /** 사용자가 적은 사유. 안 적어도 되므로 nullable이다. */
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Builder
    private BudgetChangeHistory(MonthlyBudget monthlyBudget, Long previousTotalBudget,
                                Long changedTotalBudget, BudgetChangeType changeType,
                                String changeReason) {
        this.monthlyBudget = monthlyBudget;
        this.previousTotalBudget = previousTotalBudget;
        this.changedTotalBudget = changedTotalBudget;
        this.changeType = changeType;
        this.changeReason = changeReason;
    }
}
