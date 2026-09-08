package com.dday.domain.user.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 여기 적힌 {@code message}가 프론트 화면에 그대로 뜬다 (AGENTS.md §6).
 *
 * <p>{@link #USER_NOT_FOUND}는 마이페이지에서는 사실상 "토큰은 유효한데 그 회원이 탈퇴했다"는
 * 상황이다. 401이 아니라 404인 건, 프론트가 토큰 갱신을 시도하지 말고 로그아웃시켜야 하기 때문이다.
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),

    /** 비밀번호 변경 시 현재 비밀번호가 틀렸을 때. 로그인 실패(401)와 달리 이미 인증된 상태라 400이다. */
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),

    SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "새 비밀번호가 기존 비밀번호와 같습니다."),

    PHONE_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 휴대폰 번호입니다.");

    private final HttpStatus status;
    private final String message;
}
