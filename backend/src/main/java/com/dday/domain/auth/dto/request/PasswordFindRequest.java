package com.dday.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordFindRequest {

    /** 이메일 또는 휴대폰 번호. 어느 쪽인지는 서버가 {@code @} 포함 여부로 판단한다. */
    @NotBlank(message = "이메일 또는 휴대폰 번호를 입력해주세요.")
    private String emailOrPhone;
}
