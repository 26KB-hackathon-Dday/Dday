package com.dday.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    /**
     * "로그인 상태 유지". 지금은 토큰 수명이 액세스 1년 / 리프레시 10년으로 고정이라
     * <b>서버 동작에 영향을 주지 않는다.</b> 수명을 정상화할 때 이 값으로 갈라지게 된다.
     */
    private boolean rememberMe;
}
