package com.dday.domain.pocket.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * ⚠️ 여기 적힌 {@code message}가 에러 문구의 정본이다. 프론트가 화면에 그대로 띄운다.
 */
@Getter
@RequiredArgsConstructor
public enum PocketErrorCode implements ErrorCode {

    POCKET_NOT_FOUND(HttpStatus.NOT_FOUND, "포켓을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
