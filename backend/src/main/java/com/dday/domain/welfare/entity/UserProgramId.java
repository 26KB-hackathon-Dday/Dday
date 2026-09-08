package com.dday.domain.welfare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * {@code user_program_eligibility} · {@code user_program_status}의 복합 식별자 {@code (user_id, program_id)}.
 *
 * <p>두 테이블이 같은 키 모양을 쓰므로 하나로 공유한다.
 *
 * <p>{@code userId}는 {@code users.user_id}(BIGINT)를 가리킨다 — DB FK 없이 애플리케이션 레벨 조인.
 * {@code programId}는 {@code welfare_program.serv_id}(UNIQUE)를 가리킨다. WelfareProgram 스키마가
 * 재설계 중({@code serv_id}→{@code program_id})이라 이쪽도 FK를 걸지 않는다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class UserProgramId implements Serializable {

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /** {@code welfare_program.serv_id} 참조 (예: {@code WLF00001175}). 수동 등록 제도까지 고려해 50. */
    @Column(name = "program_id", length = 50, nullable = false, updatable = false)
    private String programId;

    public UserProgramId(Long userId, String programId) {
        this.userId = userId;
        this.programId = programId;
    }
}
