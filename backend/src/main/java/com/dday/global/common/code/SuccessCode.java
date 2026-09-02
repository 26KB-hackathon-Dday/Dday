package com.dday.global.common.code;

import org.springframework.http.HttpStatus;

/**
 * 성공 응답의 {@code code} / {@code message} / HTTP 상태를 함께 들고 있는 계약.
 *
 * <p>도메인마다 {@code {도메인}SuccessCode} enum이 이 인터페이스를 구현한다.
 * 전역 단일 enum을 쓰지 않는 이유는 여러 명이 병렬로 개발할 때 그 파일에서 계속 충돌하기 때문이다.
 *
 * <p>응답의 {@code code}는 enum 상수 이름({@link Enum#name()}) 그대로 나간다.
 */
public interface SuccessCode {

    HttpStatus getStatus();

    String getMessage();

    /** enum이 자동으로 구현한다. 응답 JSON의 {@code code} 값이 된다. */
    String name();
}
