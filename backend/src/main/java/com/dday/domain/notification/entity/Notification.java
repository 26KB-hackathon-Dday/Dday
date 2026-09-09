package com.dday.domain.notification.entity;

import com.dday.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 회원 한 명에게 온 알림 한 건. SSE로 실시간 푸시하는 것과 별개로 여기 저장해둬야,
 * 구독이 끊긴 사이에 온 알림이나 지난 알림을 목록 화면에서 다시 볼 수 있다.
 *
 * <p><b>{@code type}을 enum이 아니라 문자열로 둔다.</b> 어떤 화면이 알림을 쓸지 아직
 * 정해지지 않았다 — 예산 초과, 지원제도 마감 임박, 정기수입 미입금 등 후보만 있고 확정된
 * 종류가 없다. enum으로 미리 잠그면 후보가 바뀔 때마다 마이그레이션이 필요해진다.
 * 화면이 정해지면 {@code type}별로 프론트가 아이콘·라우팅을 분기하는 계약만 문서로 남기면 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notification",
        indexes = @Index(name = "idx_notification_user", columnList = "user_id, created_at")
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 알림 종류를 가리키는 문자열 키. 화면이 정해지면 이 값으로 아이콘·라우팅을 분기한다. */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "message", length = 500)
    private String message;

    /** 알림을 눌렀을 때 이동할 프론트 라우트. 이동할 곳이 없는 알림도 있어 선택 입력이다. */
    @Column(name = "link", length = 200)
    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Notification(User user, String type, String title, String message, String link) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
        this.read = false;
    }

    public void markRead() {
        this.read = true;
    }
}
