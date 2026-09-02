package com.dday.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 실패 응답의 {@code code} / {@code message} / HTTP 상태를 함께 들고 있는 계약.
 *
 * <p><b>여기 적힌 {@code message}가 에러 계약의 정본이다.</b> 프론트가 화면에 그대로 띄우는 문구이므로
 * 임의로 바꾸면 UI 문구가 같이 바뀐다.
 *
 * <p>새 에러는 예외 클래스를 만들지 않고 {@code {도메인}ErrorCode}에 상수를 추가한다.
 * 던질 때는 언제나 {@link BusinessException} 하나만 쓴다.
 */
public interface ErrorCode {

    HttpStatus getStatus();

    String getMessage();

    /** enum이 자동으로 구현한다. 응답 JSON의 {@code code} 값이 된다. */
    String name();
}
