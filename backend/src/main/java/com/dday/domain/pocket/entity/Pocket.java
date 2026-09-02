package com.dday.domain.pocket.entity;

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
 * 목적별 포켓 하나.
 *
 * <p>{@code monthlyBudget}은 "이번 달 이 포켓에 배분하기로 확정한 금액"이고,
 * {@code spentThisMonth}는 그중 이미 쓴 금액이다. 둘의 차이가 남은 돈이다.
 * 기획서대로 <b>예산은 포켓이 초안을 제시하고 이용자가 확정</b>하므로,
 * {@code monthlyBudget}은 계산 결과가 아니라 확정된 값으로 저장한다.
 *
 * <p>⚠️ 금액은 전부 {@link BigDecimal}이다. {@code double}을 쓰면 0.1+0.2 문제가 돈에서 터진다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "pocket",
        // 한 사람이 같은 용도의 포켓을 두 개 가질 수 없다.
        // (지금은 사용자 개념이 없어 type 자체가 유일하다 — 사용자가 생기면 (userId, type)으로 바꾼다)
        uniqueConstraints = @UniqueConstraint(name = "uk_pocket_type", columnNames = "type")
)
public class Pocket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * DB에 정수 순서가 아니라 이름(HOUSING…)으로 저장한다.
     * ORDINAL로 두면 enum 상수 순서를 바꾸는 순간 기존 데이터의 의미가 통째로 어긋난다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PocketType type;

    /** 이번 달 배분액(확정값). */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyBudget;

    /** 이번 달 소진액. 마이데이터 입출금 내역이 붙으면 여기에 자동 반영된다. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal spentThisMonth;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    private Pocket(PocketType type, BigDecimal monthlyBudget, BigDecimal spentThisMonth) {
        this.type = type;
        this.monthlyBudget = monthlyBudget;
        this.spentThisMonth = spentThisMonth;
    }

    /** 남은 돈. 초과 지출이면 음수가 나온다 — 화면에서 경고로 쓰라고 일부러 자르지 않는다. */
    public BigDecimal remaining() {
        return monthlyBudget.subtract(spentThisMonth);
    }
}
