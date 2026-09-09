package com.dday.domain.notification.service;

import com.dday.domain.notification.dto.NotificationErrorCode;
import com.dday.domain.notification.dto.response.NotificationListResponse;
import com.dday.domain.notification.dto.response.NotificationResponse;
import com.dday.domain.notification.entity.Notification;
import com.dday.domain.notification.repository.NotificationRepository;
import com.dday.domain.user.dto.UserErrorCode;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 알림 발송·구독·조회. <b>{@code send()}가 이 도메인의 진입점이다.</b>
 *
 * <p>알림을 쓰는 화면(예산 초과, 지원제도 마감 임박 등)이 아직 정해지지 않아서, 다른
 * 도메인이 여기 {@code send()}만 호출하면 되게 만들어뒀다 — 저장과 실시간 푸시를
 * 한 번에 끝낸다. 구체적인 발송 조건(언제·누구에게·무슨 내용으로)은 그 도메인의 몫이다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * 알림 구독을 연다. 화면에 들어오는 즉시(예: 앱 진입 시) 한 번 불러야 하고,
     * 연결이 끊기면(네트워크 전환 등) 클라이언트의 {@code EventSource}가 알아서 다시 부른다.
     */
    public SseEmitter subscribe(Long userId) {
        return sseEmitterRegistry.register(userId);
    }

    /**
     * 알림을 만들어 저장하고, 지금 구독 중인 연결이 있으면 실시간으로도 밀어준다.
     * 구독 중이 아니어도 저장은 되니 다음에 목록을 조회하면 보인다.
     *
     * @param type    알림 종류를 가리키는 키. 화면이 정해지면 이 값으로 아이콘·라우팅을 분기한다.
     * @param link    눌렀을 때 이동할 프론트 라우트. 없으면 {@code null}.
     */
    @Transactional
    public Notification send(Long userId, String type, String title, String message, String link) {
        User user = userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        Notification notification = notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .build());

        sseEmitterRegistry.send(userId, "notification", NotificationResponse.from(notification));

        return notification;
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long userId) {
        return NotificationListResponse.of(
                notificationRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserUserIdAndReadFalse(userId);
    }

    /**
     * <b>조회 단계에서 소유자를 함께 건다</b> — id만으로 찾아 뒤에서 검사하면
     * 검사를 빠뜨린 코드가 남의 알림을 읽음으로 바꾼다.
     */
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndUserUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markRead();
    }
}
