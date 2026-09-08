package com.dday.domain.credit.entity;

import com.dday.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 신용점수 기록 한 건.
 *
 * <p>사용자당 한 행으로 덮어쓰지 않고 <b>기록을 쌓는다.</b> 점수가 올랐는지 내렸는지를
 * 보여주는 게 이 기능의 핵심이라, 최신값만 남기면 변화 그래프를 그릴 수 없다.
 * 최신 점수는 {@code (user_id, updated_at)} 인덱스로 뽑는다.
 *
 * <p>{@link #agency}가 nullable인 이유는 Mock 데이터로 시작하기 때문이다. 실제 평가기관이
 * 붙으면 KCB·NICE가 들어간다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "credit_score",
        indexes = {
                @Index(name = "idx_credit_score_user", columnList = "user_id"),
                @Index(name = "idx_credit_score_user_updated", columnList = "user_id, updated_at")
        }
)
public class CreditScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credit_score_id")
    private Long creditScoreId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 신용평가기관 (KCB, NICE …). Mock 단계에서는 비어 있다. */
    @Column(length = 50)
    private String agency;

    /** 1~1000. */
    @Column(nullable = false, columnDefinition = "SMALLINT")
    private Integer score;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private CreditScore(User user, String agency, Integer score) {
        this.user = user;
        this.agency = agency;
        this.score = score;
    }
}
