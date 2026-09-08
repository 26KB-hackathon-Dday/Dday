package com.dday.domain.user.dto.response;

import com.dday.domain.user.entity.HousingType;
import com.dday.domain.user.entity.SettlementReceived;
import com.dday.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 마이페이지 회원 정보. <b>{@code passwordHash}는 절대 담지 않는다.</b>
 *
 * <p>온보딩 전에는 프로필 값이 전부 {@code null}이다. 프론트는 {@link #onboardingCompleted}로
 * 분기하지 개별 필드의 null 여부로 판단하지 않는다 — "모르겠다"고 넘긴 항목이 있으면
 * 온보딩을 마쳤는데도 null이 남기 때문이다.
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

    /** D-day 계산 기준. 온보딩 전이면 {@code null}이다. */
    private final LocalDate protectionEndDate;

    /** 거주지 법정동 코드. */
    private final String regionCode;

    private final HousingType housingType;

    /** 온보딩 시점 보유 자산. */
    private final Long initialAsset;

    /** 주거 보증금. */
    private final Long housingDeposit;

    private final SettlementReceived settlementReceived;

    /** 마이데이터 연결 여부. 미연결이면 예산·소비 화면이 빈 값으로 뜬다. */
    private final boolean mydataConnected;

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
                .regionCode(user.getRegionCode())
                .housingType(user.getHousingType())
                .initialAsset(user.getInitialAsset())
                .housingDeposit(user.getHousingDeposit())
                .settlementReceived(user.getSettlementReceived())
                .mydataConnected(user.isMydataConnected())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
