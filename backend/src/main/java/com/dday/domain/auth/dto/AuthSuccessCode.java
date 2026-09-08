package com.dday.domain.auth.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements SuccessCode {

    VERIFICATION_CODE_SENT(HttpStatus.OK, "인증번호를 발송했습니다."),
    PHONE_VERIFIED(HttpStatus.OK, "휴대폰 인증이 완료되었습니다."),
    EMAIL_CHECKED(HttpStatus.OK, "이메일 사용 가능 여부를 확인했습니다."),
    SIGNUP_COMPLETED(HttpStatus.CREATED, "회원가입이 완료되었습니다."),
    LOGIN_SUCCEEDED(HttpStatus.OK, "로그인되었습니다."),
    LOGOUT_SUCCEEDED(HttpStatus.OK, "로그아웃되었습니다."),
    TOKEN_REISSUED(HttpStatus.OK, "액세스 토큰을 재발급했습니다."),
    PASSWORD_RESET(HttpStatus.OK, "비밀번호가 변경되었습니다.");

    private final HttpStatus status;
    private final String message;
}
