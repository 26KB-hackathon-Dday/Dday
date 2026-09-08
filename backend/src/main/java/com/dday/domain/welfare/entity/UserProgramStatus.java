package com.dday.domain.welfare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

/**
 * 유저 × 제도 수급여부 · 즐겨찾기 — <b>사용자가 소유</b>한다 (SUBSIDY-003 수급여부 확인, SUBSIDY-008 관심 제도).
 *
 * <p>배치는 이 테이블을 절대 건드리지 않는다. 자동탐지도 {@link ReceivingStatus#LIKELY_RECEIVING}으로
 * 제안만 하고, 확정은 사용자 PATCH로만. 시스템 소유 자격값은 {@link UserProgramEligibility}로 분리.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_program_status",
        indexes = {
                @Index(name = "idx_user_receiving", columnList = "user_id, receiving_status"),
                @Index(name = "idx_user_favorite", columnList = "user_id, is_favorite")
        }
)
public class UserProgramStatus {

    @EmbeddedId
    private UserProgramId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiving_status", nullable = false, length = 20)
    @ColumnDefault("'UNKNOWN'")
    private ReceivingStatus receivingStatus;

    /** {@code receiving_status == UNKNOWN}이면 {@code null}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "detection_source", length = 20)
    private DetectionSource detectionSource;

    /** {@code AUTO_DETECTED}일 때 근거가 된 거래·계좌 요약. JSON 문자열 (예: {@code {"matchedDeposits":3}}). */
    @Column(columnDefinition = "json")
    private String detectionEvidence;

    @Column(nullable = false)
    private LocalDateTime statusUpdatedAt;

    @Column(name = "is_favorite", nullable = false)
    @ColumnDefault("false")
    private boolean favorite;

    /** {@code is_favorite == false}면 {@code null}. */
    private LocalDateTime favoritedAt;

    public UserProgramStatus(Long userId, String programId) {
        this.id = new UserProgramId(userId, programId);
        this.receivingStatus = ReceivingStatus.UNKNOWN;
        this.favorite = false;
        this.statusUpdatedAt = LocalDateTime.now();
    }

    /**
     * 수급여부를 바꾼다 (SUBSIDY-003). {@code UNKNOWN}으로 되돌리면 근거도 함께 지운다.
     *
     * @param evidence {@code AUTO_DETECTED}일 때 근거 JSON. 그 외엔 호출부가 {@code null}
     */
    public void updateReceiving(ReceivingStatus status, DetectionSource source, String evidence) {
        this.receivingStatus = status;
        if (status == ReceivingStatus.UNKNOWN) {
            this.detectionSource = null;
            this.detectionEvidence = null;
        } else {
            this.detectionSource = source;
            this.detectionEvidence = source == DetectionSource.AUTO_DETECTED ? evidence : null;
        }
        this.statusUpdatedAt = LocalDateTime.now();
    }

    /** 관심 제도 등록/해제 (SUBSIDY-008). */
    public void setFavorite(boolean favorite) {
        if (this.favorite == favorite) {
            return;
        }
        this.favorite = favorite;
        this.favoritedAt = favorite ? LocalDateTime.now() : null;
        this.statusUpdatedAt = LocalDateTime.now();
    }
}
