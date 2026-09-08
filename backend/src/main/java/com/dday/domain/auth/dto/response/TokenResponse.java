package com.dday.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 액세스 토큰 재발급 응답. 리프레시 토큰은 회전시키지 않으므로 다시 내려주지 않는다. */
@Getter
@Builder
@AllArgsConstructor
public class TokenResponse {

    private final String accessToken;
}
