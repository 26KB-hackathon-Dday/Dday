package com.dday.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마이페이지 기본정보 수정. <b>PATCH라서 보내지 않은 필드({@code null})는 그대로 둔다.</b>
 * 그래서 {@code @NotBlank}가 없다 — 대신 값이 왔을 때의 형식만 검증한다.
 *
 * <p>빈 문자열("")은 검증을 통과해 이름을 지워버리므로 {@code @Size(min = 1)}로 막는다.
 *
 * <p>이메일은 여기 없다. 로그인 아이디라 변경하려면 중복 확인·재인증이 필요하고, 그건 auth 흐름이다.
 */
@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    @Size(min = 1, max = 50, message = "이름은 1자 이상 50자 이하로 입력해주세요.")
    private String name;

    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String phone;
}
