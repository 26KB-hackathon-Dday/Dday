package com.dday.domain.welfare.client;

/**
 * 복지 API 호출/파싱 실패. 배치 Step에서 이 예외로 청크 재시도를 건다.
 *
 * <p>이건 사용자 요청 처리 중 나는 예외가 아니라 배치 내부 실패라, 공통
 * {@code BusinessException} 대신 별도 예외를 쓴다.
 */
public class WelfareApiException extends RuntimeException {

    public WelfareApiException(String message) {
        super(message);
    }

    public WelfareApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
