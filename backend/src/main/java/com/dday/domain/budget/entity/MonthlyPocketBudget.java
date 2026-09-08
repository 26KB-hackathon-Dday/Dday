package com.dday.domain.budget.entity;

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
 * 어떤 달, 어떤 포켓에 얼마를 배분했는지.
 *
 * <p>포켓별 금액이지만 <b>Budget 도메인에 둔다.</b> 이 값은 포켓의 속성이 아니라 그 달 예산의
 * 일부이기 때문이다 — 포켓 행에 금액을 두면 달이 바뀔 때마다 덮어써야 해서
 * 지난달에 얼마를 배분했는지가 사라진다.
 *
 * <p>소진액은 여기 없다. 쓴 돈은 {@code financial_transaction}을 포켓·기간으로 집계해서 구한다.
 * 캐시해두면 거래가 취소·재분류될 때마다 두 값이 어긋난다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "monthly_pocket_budget",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_monthly_budget_pocket",
                columnNames = {"monthly_budget_id", "pocket_id"}
        )
)
public class MonthlyPocketBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_pocket_budget_id")
    private Long monthlyPocketBudgetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_budget_id", nullable = false)
    private MonthlyBudget monthlyBudget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pocket_id", nullable = false)
    private Pocket pocket;

    /** 이 달 이 포켓의 목표 금액. */
    @Column(name = "target_amount", nullable = false)
    private Long targetAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_method", nullable = false, length = 20)
    private AllocationMethod allocationMethod;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private MonthlyPocketBudget(MonthlyBudget monthlyBudget, Pocket pocket, Long targetAmount,
                                AllocationMethod allocationMethod) {
        this.monthlyBudget = monthlyBudget;
        this.pocket = pocket;
        this.targetAmount = targetAmount != null ? targetAmount : 0L;
        this.allocationMethod = allocationMethod;
    }

    /** 사용자가 직접 금액을 정하면 배분 방식도 함께 바뀐다. 잔여 자동배정 대상에서 빠진다. */
    public void changeTargetAmount(Long targetAmount) {
        this.targetAmount = targetAmount;
        this.allocationMethod = AllocationMethod.USER_INPUT;
    }

    /** 남은 예산을 흘려넣는다. 다른 포켓 금액이 바뀔 때마다 다시 계산된다. */
    public void absorbRemainder(Long targetAmount) {
        this.targetAmount = targetAmount;
        this.allocationMethod = AllocationMethod.AUTO_REMAINDER;
    }
}
