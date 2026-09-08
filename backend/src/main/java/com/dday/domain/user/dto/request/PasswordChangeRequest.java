package com.dday.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인한 회원이 스스로 비밀번호를 바꾼다. 휴대폰 인증으로 재설정하는
 * {@code /api/auth/password/reset}과 달리 <b>현재 비밀번호를 알아야 한다.</b>
 * (토큰이 탈취된 상황에서 비밀번호까지 바뀌면 계정을 되찾을 방법이 없어진다)
 */
@Getter
@NoArgsConstructor
public class PasswordChangeRequest {

    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해주세요.")
    private String newPassword;
}
