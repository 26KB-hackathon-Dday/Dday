package com.dday.domain.notification.service;

import com.dday.domain.notification.dto.NotificationErrorCode;
import com.dday.domain.notification.entity.Notification;
import com.dday.domain.notification.repository.NotificationRepository;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    private NotificationService notificationService;
    private User user;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository, userRepository, sseEmitterRegistry);

        user = User.builder().email("user1@test.com").build();
        ReflectionTestUtils.setField(user, "userId", USER_ID);
    }

    @Test
    void 알림을_보내면_저장하고_구독중인_연결에도_실시간으로_밀어준다() {
        given(userRepository.findByUserIdAndStatus(USER_ID, UserStatus.ACTIVE))
                .willReturn(Optional.of(user));
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.send(
                USER_ID, "BUDGET_EXCEEDED", "예산을 넘었어요", "이번 달 자유 포켓을 다 썼어요.", "/pockets");

        assertThat(result.getType()).isEqualTo("BUDGET_EXCEEDED");
        assertThat(result.getTitle()).isEqualTo("예산을 넘었어요");
        verify(sseEmitterRegistry).send(eq(USER_ID), eq("notification"), any());
    }

    @Test
    void 남의_알림이든_없는_id든_읽음처리는_같은_404를_던진다() {
        given(notificationRepository.findByNotificationIdAndUserUserId(99L, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(USER_ID, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void 본인_알림이면_읽음처리된다() {
        Notification notification = Notification.builder()
                .user(user)
                .type("BUDGET_EXCEEDED")
                .title("예산을 넘었어요")
                .build();
        given(notificationRepository.findByNotificationIdAndUserUserId(1L, USER_ID))
                .willReturn(Optional.of(notification));

        notificationService.markRead(USER_ID, 1L);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository, never()).save(any());
    }
}
