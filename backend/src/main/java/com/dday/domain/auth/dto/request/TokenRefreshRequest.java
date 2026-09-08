package com.dday.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenRefreshRequest {

    @NotBlank(message = "리프레시 토큰이 필요합니다.")
    private String refreshToken;
}
