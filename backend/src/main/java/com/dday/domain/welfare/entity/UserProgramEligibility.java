package com.dday.domain.welfare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 유저 × 제도 자격 판별 결과 — <b>시스템(SUBSIDY-002 배치)이 소유</b>한다.
 *
 * <p>사용자 액션으로는 절대 안 바뀐다. 배치가 언제든 덮어써도 되는 값만 있다.
 * 수급여부·즐겨찾기(사용자 소유)는 {@link UserProgramStatus}로 분리 — 자격 재계산이
 * 사용자 입력을 지우는 사고를 막기 위해서다 (같은 복합키, 다른 라이프사이클).
 *
 * <p>({@code user}/{@code member} 테이블이 아직 없어 {@code user_id} FK는 못 건다 — soft reference.
 * 조회는 {@code program_id}로 {@code welfare_program}과 애플리케이션 레벨 조인.)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_program_eligibility",
        indexes = @Index(name = "idx_user_eligible", columnList = "user_id, eligible")
)
public class UserProgramEligibility {

    @EmbeddedId
    private UserProgramId id;

    /** 자격 여부 판별 결과. */
    @Column(nullable = false)
    private boolean eligible;

    /** 충족된 조건 키 목록. JSON 배열 문자열 (예: {@code ["protectionEndDate","region"]}). */
    @Column(columnDefinition = "json")
    private String matchedCriteria;

    /** 불일치 사유. {@code eligible == true}면 {@code null}. */
    @Column(columnDefinition = "TEXT")
    private String ineligibleReason;

    /** 마지막 재판별 시각. 배치가 매 실행마다 갱신한다. */
    @Column(nullable = false)
    private LocalDateTime evaluatedAt;

    public UserProgramEligibility(UUID userId, String programId) {
        this.id = new UserProgramId(userId, programId);
        this.eligible = false;
        this.evaluatedAt = LocalDateTime.now();
    }

    /**
     * SUBSIDY-002 배치가 재판별 결과를 반영한다. 매번 무조건 덮어쓴다.
     *
     * @param matchedCriteria 충족 조건 JSON 배열 문자열 ({@code null} 허용)
     * @param ineligibleReason 불일치 사유. {@code eligible}이면 호출부가 {@code null}로 넘긴다
     */
    public void applyEvaluation(boolean eligible, String matchedCriteria, String ineligibleReason) {
        this.eligible = eligible;
        this.matchedCriteria = matchedCriteria;
        this.ineligibleReason = eligible ? null : ineligibleReason;
        this.evaluatedAt = LocalDateTime.now();
    }
}
