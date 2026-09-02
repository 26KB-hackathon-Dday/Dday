package com.dday.global.exception;

import lombok.Getter;

/**
 * 이 프로젝트의 <b>유일한</b> 비즈니스 예외다. 도메인별 예외 클래스를 만들지 않는다.
 *
 * <p>실패 응답에 부가 데이터가 필요하면(예: 남은 시도 횟수) 2-인자 생성자를 쓴다.
 * 쓰지 않으면 응답의 {@code data}는 {@code null}로 나간다.
 *
 * <pre>{@code
 * throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
 * throw new BusinessException(AuthErrorCode.PIN_MISMATCH, new RemainingAttempts(2));
 * }</pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final transient ErrorCode errorCode;

    /** 실패 응답의 {@code data}에 실릴 부가 데이터. 없으면 {@code null}. */
    private final transient Object data;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public BusinessException(ErrorCode errorCode, Object data) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = data;
    }
}
