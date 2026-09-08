package com.dday.domain.auth.dto.response;

import com.dday.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final UserSummary user;

    public static LoginResponse of(User user, String accessToken, String refreshToken) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserSummary.from(user))
                .build();
    }

    /**
     * 로그인 직후 화면을 고르는 데 필요한 최소 정보만 담는다.
     * {@code onboardingCompleted}가 false면 프론트는 온보딩으로 보낸다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class UserSummary {

        private final Long userId;
        private final String name;
        private final boolean onboardingCompleted;

        public static UserSummary from(User user) {
            return UserSummary.builder()
                    .userId(user.getUserId())
                    .name(user.getName())
                    .onboardingCompleted(user.isOnboardingCompleted())
                    .build();
        }
    }
}
