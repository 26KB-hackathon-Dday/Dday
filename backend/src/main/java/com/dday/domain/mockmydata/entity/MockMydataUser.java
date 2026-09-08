package com.dday.domain.mockmydata.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Mock 금융기관 쪽 사용자. 실제 마이데이터 서버가 들고 있을 사용자를 흉내낸다.
 *
 * <p>{@link #serviceUserId}는 우리 {@code users.user_id}와 논리적으로 짝이지만
 * <b>FK를 걸지 않는다.</b> 이 도메인은 "외부 기관"인 척해야 하고, 외부 기관이 우리 DB의
 * 회원 테이블을 참조할 수는 없기 때문이다. 나중에 진짜 API로 바꿀 때 이 도메인을
 * 통째로 들어내도 나머지가 안 깨지는 것도 같은 이유다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "mock_mydata_user",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mock_service_user",
                columnNames = "service_user_id"
        )
)
public class MockMydataUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mock_user_id")
    private Long mockUserId;

    /** 서비스 {@code users.user_id}에 대응하는 값. FK가 아니라 논리적 연결이다. */
    @Column(name = "service_user_id", nullable = false)
    private Long serviceUserId;

    @Column(nullable = false, length = 50)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private MockMydataUser(Long serviceUserId, String name) {
        this.serviceUserId = serviceUserId;
        this.name = name;
    }
}
