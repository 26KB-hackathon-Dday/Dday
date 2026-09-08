package com.dday.domain.auth.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 여기 적힌 {@code message}가 프론트 화면에 그대로 뜬다 (AGENTS.md §6).
 *
 * <p>{@link #INVALID_CREDENTIAL}이 "이메일이 없다"와 "비밀번호가 틀렸다"를 구분하지 않는 건
 * 의도한 것이다. 구분해 주면 공격자가 가입된 이메일 목록을 뽑아낼 수 있다.
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관에 동의해야 가입할 수 있습니다."),
    INVALID_CREDENTIAL(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

    VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "인증번호를 먼저 발송해주세요."),
    VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다. 다시 발송해주세요."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다."),

    /** 인증을 아예 안 했거나, 인증하고 너무 오래 지나 가입 단계에서 다시 받아야 할 때. */
    PHONE_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "휴대폰 인증을 먼저 완료해주세요."),

    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "다시 로그인해주세요."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "가입된 회원 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
