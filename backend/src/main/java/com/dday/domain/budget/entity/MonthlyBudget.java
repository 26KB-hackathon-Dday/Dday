package com.dday.domain.budget.entity;

import com.dday.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자의 한 달치 전체 예산.
 *
 * <p>{@link #budgetMonth}는 <b>그 달 1일</b>을 넣는다. MySQL에 '연월' 타입이 없어서 DATE에
 * 담는데, 날짜를 제각각 넣으면 같은 달인데 다른 행이 생긴다. 1일로 고정하면
 * {@code (user, budget_month)} 유일 제약이 "한 사람당 한 달에 하나"를 그대로 보장한다.
 *
 * <p>포켓별 배분액은 여기 없고 {@link MonthlyPocketBudget}에 한 줄씩 붙는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "monthly_budget",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_budget_month",
                columnNames = {"user_id", "budget_month"}
        )
)
public class MonthlyBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_budget_id")
    private Long monthlyBudgetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 기준 월. 반드시 해당 월 1일을 넣는다. */
    @Column(name = "budget_month", nullable = false)
    private LocalDate budgetMonth;

    /** 이번 달 쓸 수 있는 돈 전부. 원 단위 정수다. */
    @Column(name = "total_budget_amount", nullable = false)
    private Long totalBudgetAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_status", nullable = false, length = 20)
    private BudgetStatus budgetStatus;

    /** "월세가 올라 필수 포켓을 3만원 늘렸어요" 같은 한 문단. 화면에 그대로 띄운다. */
    @Column(name = "draft_summary", length = 1000)
    private String draftSummary;

    @Column(name = "draft_created_at")
    private LocalDateTime draftCreatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private MonthlyBudget(User user, LocalDate budgetMonth, Long totalBudgetAmount,
                          String draftSummary, LocalDateTime draftCreatedAt) {
        this.user = user;
        this.budgetMonth = budgetMonth;
        this.totalBudgetAmount = totalBudgetAmount;
        this.draftSummary = draftSummary;
        this.draftCreatedAt = draftCreatedAt;
        this.budgetStatus = BudgetStatus.DRAFT;
    }

    /**
     * 초안을 다시 계산해 덮어쓴다. <b>이미 확정된 예산은 건드리지 않는다</b> —
     * 사용자가 정한 금액을 시스템이 조용히 바꾸면 신뢰가 깨진다.
     */
    public void replaceDraft(Long totalBudgetAmount, String draftSummary,
                             LocalDateTime draftCreatedAt) {
        if (this.budgetStatus == BudgetStatus.CONFIRMED) {
            return;
        }
        this.totalBudgetAmount = totalBudgetAmount;
        this.draftSummary = draftSummary;
        this.draftCreatedAt = draftCreatedAt;
    }

    public void confirm(LocalDateTime confirmedAt) {
        this.budgetStatus = BudgetStatus.CONFIRMED;
        this.confirmedAt = confirmedAt;
    }

    /** 확정 후 사용자가 총액을 직접 고치는 경우. 변경 이력은 서비스가 따로 남긴다. */
    public void changeTotalAmount(Long totalBudgetAmount) {
        this.totalBudgetAmount = totalBudgetAmount;
    }

    public boolean isConfirmed() {
        return this.budgetStatus == BudgetStatus.CONFIRMED;
    }
}
