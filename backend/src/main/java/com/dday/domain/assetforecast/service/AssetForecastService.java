package com.dday.domain.assetforecast.service;

import com.dday.domain.assetforecast.dto.response.AssetForecastResponse;
import com.dday.domain.assetforecast.repository.AssetForecastAccountRepository;
import com.dday.domain.assetforecast.repository.AssetForecastPocketBudgetRepository;
import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.mydata.entity.AccountType;
import com.dday.domain.onboarding.service.OnboardingCalculator;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.user.dto.UserErrorCode;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import com.dday.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetForecastService {

    private final UserRepository
            userRepository;

    private final AssetForecastAccountRepository
            assetForecastAccountRepository;

    private final AssetForecastPocketBudgetRepository
            assetForecastPocketBudgetRepository;

    /**
     * 지원 종료 시 예상 총자산 조회.
     *
     * 현재 미래자산
     * = SAVINGS + INVESTMENT 계좌 잔액
     *
     * 앞으로 쌓일 예상 자산
     * = 현재 월 FUTURE_ASSET 포켓 배정액
     *   × 지원 종료까지 남은 개월 수
     *
     * 예상 총자산
     * = 현재 미래자산
     *   + 앞으로 쌓일 예상 자산
     *
     * 투자 수익률은 반영하지 않는다.
     */
    public AssetForecastResponse getForecast(
            Long userId
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                UserErrorCode.USER_NOT_FOUND
                                        )
                        );

        LocalDate today =
                LocalDate.now();

        LocalDate protectionEndDate =
                user.getProtectionEndDate();

        /*
         * 보호종료일이 없으면
         * 지원 종료일과 남은 개월 수를
         * 계산할 수 없다.
         */
        if (protectionEndDate == null) {

            throw new BusinessException(
                    CommonErrorCode.INVALID_INPUT_VALUE
            );
        }

        LocalDate supportEndDate =
                OnboardingCalculator
                        .supportEndDate(
                                protectionEndDate
                        );

        long calculatedRemainingMonths =
                OnboardingCalculator
                        .remainingMonths(
                                protectionEndDate,
                                today
                        );

        /*
         * 이미 지원 기간이 끝난 경우
         * 앞으로 적립 가능한 개월 수는 0으로 본다.
         */
        long remainingMonths =
                Math.max(
                        calculatedRemainingMonths,
                        0L
                );

        /*
         * 현재 미래자산:
         *
         * 활성 SAVINGS +
         * 활성 INVESTMENT 계좌 잔액.
         */
        Long currentAsset =
                assetForecastAccountRepository
                        .sumCurrentFutureAsset(
                                userId,
                                List.of(
                                        AccountType.SAVINGS,
                                        AccountType.INVESTMENT
                                )
                        );

        if (currentAsset == null) {
            currentAsset = 0L;
        }

        /*
         * 이번 달 FUTURE_ASSET
         * 포켓 배정액 조회.
         */
        LocalDate budgetMonth =
                today.withDayOfMonth(1);

        Long monthlyFutureAmount =
                assetForecastPocketBudgetRepository
                        .findByUserAndMonthAndPocketType(
                                userId,
                                budgetMonth,
                                PocketType.FUTURE_ASSET
                        )
                        .map(
                                MonthlyPocketBudget::getTargetAmount
                        )
                        .orElse(0L);

        /*
         * 지원 종료까지 추가될
         * 예상 자산.
         */
        long expectedAdditionalAsset =
                monthlyFutureAmount
                        * remainingMonths;

        /*
         * 지원 종료 시 예상 총자산.
         */
        long expectedAsset =
                currentAsset
                        + expectedAdditionalAsset;

        return AssetForecastResponse
                .builder()
                .currentAsset(
                        currentAsset
                )
                .monthlyFutureAmount(
                        monthlyFutureAmount
                )
                .remainingMonths(
                        remainingMonths
                )
                .supportEndDate(
                        supportEndDate
                )
                .expectedAdditionalAsset(
                        expectedAdditionalAsset
                )
                .expectedAsset(
                        expectedAsset
                )
                .investmentReturnIncluded(
                        false
                )
                .build();
    }
}