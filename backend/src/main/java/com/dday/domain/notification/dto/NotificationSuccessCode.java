package com.dday.domain.notification.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationSuccessCode implements SuccessCode {

    NOTIFICATIONS_FOUND(HttpStatus.OK, "알림을 조회했습니다."),
    UNREAD_COUNT_FOUND(HttpStatus.OK, "안 읽은 알림 수를 조회했습니다."),
    NOTIFICATION_READ(HttpStatus.OK, "알림을 읽음 처리했습니다.");

    private final HttpStatus status;
    private final String message;
}
