package com.dday.domain.notification.controller;

import com.dday.domain.notification.dto.NotificationSuccessCode;
import com.dday.domain.notification.dto.response.NotificationListResponse;
import com.dday.domain.notification.dto.response.UnreadCountResponse;
import com.dday.domain.notification.service.NotificationService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 알림. 어떤 화면이 알림을 만들어낼지는 아직 정해지지 않았고, 여기는 그 화면들이 공통으로
 * 쓸 구독·조회·읽음처리만 담당한다 — 발송 조건은 각 도메인이 {@code NotificationService.send()}를
 * 부르는 쪽에서 정한다.
 */
@Tag(name = "알림")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 실시간 구독(SSE)", description = """
            앱 진입 시 한 번 연다. 연결이 끊기면 브라우저의 `EventSource`가 알아서 재연결한다.

            **인증 방법이 다르다.** `EventSource`는 커스텀 헤더를 못 보내서, 이 경로만
            `Authorization` 헤더 대신 `?token=` 쿼리 파라미터로 액세스 토큰을 받는다.
            예) `new EventSource('/api/notifications/subscribe?token=' + accessToken)`

            이벤트 이름은 `notification`이고 데이터는 알림 한 건(JSON)이다. 연결 직후
            `connect` 이벤트가 한 번 오는데, 이건 프록시 버퍼링 방지용이라 무시해도 된다.
            """)
    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal Long userId) {
        return notificationService.subscribe(userId);
    }

    @Operation(summary = "알림 목록 조회", description = "최신순. 읽었는지 여부(`read`)도 함께 준다.")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(NotificationSuccessCode.NOTIFICATIONS_FOUND,
                notificationService.getNotifications(userId));
    }

    @Operation(summary = "안 읽은 알림 수 조회", description = "하단 네비 배지 등에 쓴다.")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(NotificationSuccessCode.UNREAD_COUNT_FOUND,
                UnreadCountResponse.of(notificationService.getUnreadCount(userId)));
    }

    @Operation(summary = "알림 읽음 처리", description = """
            본인 것만 읽음 처리할 수 있다. 남의 알림이든 없는 id든 **같은 404**를 준다.

            | HTTP | code | message |
            |---|---|---|
            | 404 | NOTIFICATION_NOT_FOUND | 알림을 찾을 수 없습니다. |
            """)
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "알림 ID") @PathVariable Long notificationId) {
        notificationService.markRead(userId, notificationId);
        return ApiResponse.of(NotificationSuccessCode.NOTIFICATION_READ);
    }
}
