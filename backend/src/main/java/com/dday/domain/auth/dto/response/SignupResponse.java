package com.dday.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 가입 직후 바로 로그인된 상태로 만들기 위해 토큰까지 같이 돌려준다. */
@Getter
@Builder
@AllArgsConstructor
public class SignupResponse {

    private final Long userId;
    private final String accessToken;
    private final String refreshToken;
}
