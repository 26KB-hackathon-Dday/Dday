package com.dday.global.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 도메인에 속하지 않는 공통 성공 코드. 도메인 응답은 {@code {도메인}SuccessCode}를 따로 만든다. */
@Getter
@RequiredArgsConstructor
public enum CommonSuccessCode implements SuccessCode {

    OK(HttpStatus.OK, "요청이 정상 처리되었습니다."),
    HEALTHY(HttpStatus.OK, "서버가 정상 동작 중입니다."),
    DB_HEALTHY(HttpStatus.OK, "데이터베이스 연결이 정상입니다.");

    private final HttpStatus status;
    private final String message;
}
