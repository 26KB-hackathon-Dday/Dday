package com.dday.domain.user.dto.response;

import com.dday.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 마이페이지 회원 정보. <b>{@code passwordHash}는 절대 담지 않는다.</b>
 *
 * <p>온보딩·프로필 값(거주지역, 주거형태, 초기자산 등)은 아직 {@code User}에 없다.
 * 온보딩 도메인이 컬럼을 붙이면 여기에 필드를 더하면 된다.
 */
@Getter
@Builder
@AllArgsConstructor
public class UserResponse {

    private final Long userId;
    private final String email;
    private final String name;
    private final String phone;
    private final boolean agreedLocation;

    /** 프론트가 온보딩으로 보낼지 홈으로 보낼지 판단하는 값. */
    private final boolean onboardingCompleted;

    /** 온보딩 전에는 {@code null}이다. */
    private final LocalDateTime protectionEndDate;

    private final LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .agreedLocation(user.isAgreedLocation())
                .onboardingCompleted(user.isOnboardingCompleted())
                .protectionEndDate(user.getProtectionEndDate())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
