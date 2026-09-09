package com.dday.domain.notification.repository;

import com.dday.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 최신순. 화면은 최근 온 알림부터 보여준다. */
    List<Notification> findAllByUserUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * <b>id만으로 찾지 않고 소유자를 함께 건다.</b> 읽음 처리에서 이 메서드를 쓰면
     * 남의 알림을 읽음으로 바꾸는 요청이 조회 단계에서 빈 값이 되어 애초에 진행되지 않는다.
     */
    Optional<Notification> findByNotificationIdAndUserUserId(Long notificationId, Long userId);

    long countByUserUserIdAndReadFalse(Long userId);
}
