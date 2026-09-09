package com.dday.domain.asset.service;

import com.dday.domain.asset.dto.response.AssetForecastResponse;
import com.dday.domain.mydata.repository.UserAccountRepository;
import com.dday.domain.onboarding.service.OnboardingCalculator;
import com.dday.domain.user.dto.UserErrorCode;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AssetForecastService {

    private final UserRepository userRepository;

    private final UserAccountRepository userAccountRepository;

    /**
     * 재조정 화면의 예상 자산 계산에 필요한
     * 현재 자산과 남은 지원 개월 수를 조회한다.
     */
    @Transactional(readOnly = true)
    public AssetForecastResponse find(
            Long userId
    ) {
        User user =
                userRepository
                        .findByUserIdAndStatus(
                                userId,
                                UserStatus.ACTIVE
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        UserErrorCode.USER_NOT_FOUND
                                )
                        );

        long currentAsset =
                calculateCurrentAsset(
                        user
                );

        long remainingMonths =
                calculateRemainingMonths(
                        user
                );

        return AssetForecastResponse
                .builder()
                .currentAsset(
                        currentAsset
                )
                .remainingMonths(
                        remainingMonths
                )
                .build();
    }

    /**
     * 현재 자산 계산.
     *
     * 1순위:
     * 사용자가 선택한 활성 마이데이터 계좌 잔액 합계
     *
     * 2순위:
     * 선택 계좌가 없으면 온보딩에서 입력한 initialAsset
     */
    private long calculateCurrentAsset(
            User user
    ) {
        Long accountBalance =
                userAccountRepository
                        .sumSelectedActiveBalance(
                                user.getUserId()
                        );

        long selectedBalance =
                accountBalance == null
                        ? 0L
                        : accountBalance;

        if (
                selectedBalance > 0
        ) {
            return selectedBalance;
        }

        Long initialAsset =
                user.getInitialAsset();

        return initialAsset == null
                ? 0L
                : Math.max(
                initialAsset,
                0L
        );
    }

    /**
     * 지원 종료까지 남은 개월 수.
     *
     * 기존 프로젝트의 계산 기준을 그대로 사용한다.
     *
     * 보호종료일 + 5년까지 남은 개월 수.
     */
    private long calculateRemainingMonths(
            User user
    ) {
        LocalDate protectionEndDate =
                user.getProtectionEndDate();

        /*
         * 보호종료일을 아직 입력하지 않은 경우
         * 계산할 기준이 없으므로 0개월로 처리.
         */
        if (
                protectionEndDate == null
        ) {
            return 0L;
        }

        long remainingMonths =
                OnboardingCalculator
                        .remainingMonths(
                                protectionEndDate,
                                LocalDate.now()
                        );

        /*
         * 지원기간이 이미 끝났으면
         * 예상 자산 계산에서 음수 개월을 곱하면 안 되므로
         * 0개월로 보정.
         */
        return Math.max(
                remainingMonths,
                0L
        );
    }
}