package com.dday.domain.welfare.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 지원금 매칭 도메인의 실패 코드. {@code message}가 프론트 화면 문구의 정본이다 (AGENTS.md §5).
 */
@Getter
@RequiredArgsConstructor
public enum WelfareErrorCode implements ErrorCode {

    PROGRAM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 지원제도입니다."),
    INELIGIBLE_PROGRAM(HttpStatus.UNPROCESSABLE_ENTITY, "자격이 확인되지 않은 제도입니다."),
    SUBSIDY_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
