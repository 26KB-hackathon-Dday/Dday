package com.dday.domain.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code agreedTerms}·{@code agreedPrivacy}에 {@code @AssertTrue}를 걸지 않은 건 의도한 것이다.
 * 그렇게 하면 미동의가 필드 검증 오류(INVALID_INPUT_VALUE)로 나가는데, 프론트는 이걸
 * "약관 화면으로 되돌리기"라는 별도 흐름으로 다뤄야 해서 서비스에서 TERMS_NOT_AGREED로 던진다.
 */
@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
    private String name;

    @NotBlank(message = "휴대폰 번호를 입력해주세요.")
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String phone;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 255, message = "이메일은 255자 이하로 입력해주세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해주세요.")
    private String password;

    /** 이용약관 동의 (필수) */
    private boolean agreedTerms;

    /** 개인정보 수집·이용 동의 (필수) */
    private boolean agreedPrivacy;

    /** 위치정보 동의 (선택) */
    private boolean agreedLocation;
}
