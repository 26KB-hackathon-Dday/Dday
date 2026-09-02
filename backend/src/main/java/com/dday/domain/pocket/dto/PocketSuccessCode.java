package com.dday.domain.pocket.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PocketSuccessCode implements SuccessCode {

    POCKETS_FOUND(HttpStatus.OK, "포켓 목록을 조회했습니다."),
    POCKET_FOUND(HttpStatus.OK, "포켓을 조회했습니다.");

    private final HttpStatus status;
    private final String message;
}
