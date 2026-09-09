package com.dday.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 하단 네비 배지 등에 쓰는 안 읽은 알림 개수. */
@Getter
@Builder
@AllArgsConstructor
public class UnreadCountResponse {

    private final long unreadCount;

    public static UnreadCountResponse of(long unreadCount) {
        return UnreadCountResponse.builder().unreadCount(unreadCount).build();
    }
}
