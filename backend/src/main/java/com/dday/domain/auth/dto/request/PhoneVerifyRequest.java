package com.dday.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PhoneVerifyRequest {

    @NotBlank(message = "휴대폰 번호를 입력해주세요.")
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String phone;

    @NotBlank(message = "인증번호를 입력해주세요.")
    @Pattern(regexp = "^\\d{6}$", message = "인증번호는 숫자 6자리입니다.")
    private String code;
}
