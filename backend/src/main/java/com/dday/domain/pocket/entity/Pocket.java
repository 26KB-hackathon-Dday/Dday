package com.dday.domain.pocket.entity;

import com.dday.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 목적별 포켓 하나. 사용자당 {@link PocketType} 네 개가 고정으로 생긴다.
 *
 * <p><b>포켓은 금액을 들지 않는다.</b> 이번 달 목표 금액은 월마다 달라지므로
 * {@code monthly_pocket_budget}(Budget 도메인)이 들고, 실제 소진액은
 * {@code financial_transaction}을 이 포켓으로 집계해서 구한다. 포켓 행에 금액을 두면
 * 달이 바뀔 때마다 덮어써야 해서 지난달 기록이 남지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "pocket",
        // 한 사람이 같은 용도의 포켓을 두 개 가질 수 없다.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_pocket_type",
                columnNames = {"user_id", "pocket_type"}
        )
)
public class Pocket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pocket_id")
    private Long pocketId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * DB에 정수 순서가 아니라 이름(ESSENTIAL…)으로 저장한다.
     * ORDINAL로 두면 enum 상수 순서를 바꾸는 순간 기존 데이터의 의미가 통째로 어긋난다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pocket_type", nullable = false, length = 20)
    private PocketType pocketType;

    /** 화면에 그대로 노출되는 이름. 사용자가 바꿀 수 있어서 enum이 아니라 행에 둔다. */
    @Column(name = "pocket_name", nullable = false, length = 50)
    private String pocketName;

    /** 이 포켓이 무엇을 담는지에 대한 한 줄 설명. 역시 화면에 그대로 쓴다. */
    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Pocket(User user, PocketType pocketType, String pocketName, String description) {
        this.user = user;
        this.pocketType = pocketType;
        this.pocketName = pocketName;
        this.description = description;
    }

    /** 포켓 이름·설명 변경. {@code null}은 "안 바꾼다"는 뜻이라 건너뛴다. */
    public void rename(String pocketName, String description) {
        if (pocketName != null) {
            this.pocketName = pocketName;
        }
        if (description != null) {
            this.description = description;
        }
    }
}
