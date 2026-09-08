package com.dday.domain.welfare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.UUID;

/**
 * {@code user_program_eligibility} · {@code user_program_status}의 복합 식별자 {@code (user_id, program_id)}.
 *
 * <p>두 테이블이 같은 키 모양을 쓰므로 하나로 공유한다.
 *
 * <p>{@code userId}는 아직 {@code user}/{@code member} 테이블이 없어 FK를 못 건다 — soft reference.
 * {@code programId}는 {@code welfare_program.serv_id}(UNIQUE)를 가리키지만, WelfareProgram 스키마가
 * 재설계 중({@code serv_id}→{@code program_id})이라 이쪽도 DB FK 없이 애플리케이션 레벨 조인.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class UserProgramId implements Serializable {

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_id", length = 36, nullable = false, updatable = false)
    private UUID userId;

    /** {@code welfare_program.serv_id} 참조 (예: {@code WLF00001175}). 수동 등록 제도까지 고려해 50. */
    @Column(name = "program_id", length = 50, nullable = false, updatable = false)
    private String programId;

    public UserProgramId(UUID userId, String programId) {
        this.userId = userId;
        this.programId = programId;
    }
}
