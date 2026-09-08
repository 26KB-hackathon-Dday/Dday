package com.dday.domain.budget.entity;

import com.dday.domain.pocket.entity.Pocket;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예산 변경 한 건에서 포켓 하나가 얼마에서 얼마로 바뀌었는지.
 *
 * <p>{@link BudgetChangeHistory} 한 줄에 이 상세가 여러 줄 붙는다. 자유 포켓에서 비상금으로
 * 5만원을 옮겼다면 상세 두 줄(자유 -5만, 비상금 +5만)이 같은 이력에 매달린다.
 *
 * <p>포켓 FK에 {@code ON DELETE CASCADE}를 걸지 않는다 — 포켓이 지워졌다고 과거 예산
 * 변경 기록까지 사라지면 결산이 맞지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "budget_change_detail")
public class BudgetChangeDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_change_detail_id")
    private Long budgetChangeDetailId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_change_history_id", nullable = false)
    private BudgetChangeHistory budgetChangeHistory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pocket_id", nullable = false)
    private Pocket pocket;

    @Column(name = "previous_amount", nullable = false)
    private Long previousAmount;

    @Column(name = "changed_amount", nullable = false)
    private Long changedAmount;

    @Builder
    private BudgetChangeDetail(BudgetChangeHistory budgetChangeHistory, Pocket pocket,
                               Long previousAmount, Long changedAmount) {
        this.budgetChangeHistory = budgetChangeHistory;
        this.pocket = pocket;
        this.previousAmount = previousAmount;
        this.changedAmount = changedAmount;
    }

    /** 증감액. 옮겨나간 포켓이면 음수다. */
    public long delta() {
        return this.changedAmount - this.previousAmount;
    }
}
