package com.dday.domain.auth.entity;

/** 휴대폰 인증을 무엇 때문에 받았는지. 가입용 코드로 비밀번호를 바꾸지 못하게 막는 구분자다. */
public enum VerificationPurpose {

    SIGNUP,
    PASSWORD_RESET
}
