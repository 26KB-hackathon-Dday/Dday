package com.dday.domain.notification.dto.response;

import com.dday.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class NotificationListResponse {

    private final List<NotificationResponse> notifications;

    public static NotificationListResponse of(List<Notification> notifications) {
        return NotificationListResponse.builder()
                .notifications(notifications.stream().map(NotificationResponse::from).toList())
                .build();
    }
}
