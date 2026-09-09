package com.dday.domain.notification.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    SSE_SUBSCRIBE_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "알림 구독에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
