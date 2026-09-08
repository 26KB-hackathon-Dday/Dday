package com.dday.domain.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 탈퇴 사유는 선택 입력이다 — 사유를 강제하면 탈퇴 자체를 막는 다크패턴이 된다.
 * 본문 없이 {@code DELETE /api/users/me}만 불러도 통과한다.
 */
@Getter
@NoArgsConstructor
public class UserWithdrawRequest {

    @Size(max = 500, message = "탈퇴 사유는 500자 이하로 입력해주세요.")
    private String reason;
}
