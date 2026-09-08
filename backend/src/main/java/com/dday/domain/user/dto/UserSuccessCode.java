package com.dday.domain.user.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserSuccessCode implements SuccessCode {

    USER_FOUND(HttpStatus.OK, "회원 정보를 조회했습니다."),
    USER_UPDATED(HttpStatus.OK, "회원 정보를 수정했습니다."),
    PASSWORD_CHANGED(HttpStatus.OK, "비밀번호가 변경되었습니다."),
    USER_WITHDRAWN(HttpStatus.OK, "탈퇴가 완료되었습니다.");

    private final HttpStatus status;
    private final String message;
}
