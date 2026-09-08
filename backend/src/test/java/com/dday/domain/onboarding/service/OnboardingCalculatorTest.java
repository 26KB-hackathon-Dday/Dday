package com.dday.domain.onboarding.service;

import com.dday.domain.onboarding.dto.response.ProtectionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D-day 계산은 보호종료일 저장(2번)과 온보딩 완료(10번) 두 곳에서 쓰인다.
 * 여기가 틀리면 화면마다 다른 D-day가 뜬다.
 */
class OnboardingCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 8);

    @Test
    void 지원_종료일은_보호종료일의_5년_뒤다() {
        assertThat(OnboardingCalculator.supportEndDate(LocalDate.of(2025, 3, 1)))
                .isEqualTo(LocalDate.of(2030, 3, 1));
    }

    @Test
    void 윤년의_2월_29일은_5년_뒤_2월_28일이_된다() {
        // plusYears는 존재하지 않는 날짜를 그 달의 말일로 당긴다. 예외가 아니라 정상 동작이다.
        assertThat(OnboardingCalculator.supportEndDate(LocalDate.of(2024, 2, 29)))
                .isEqualTo(LocalDate.of(2029, 2, 28));
    }

    @Test
    void dDay는_오늘부터_지원_종료일까지_남은_일수다() {
        // 보호종료 2025-03-01 → 지원종료 2030-03-01. 오늘(2026-09-08)부터 1270일.
        // 계산식이 아니라 미리 센 숫자를 박는다 — 같은 식으로 기댓값을 만들면 검증이 아니다.
        assertThat(OnboardingCalculator.dDay(LocalDate.of(2025, 3, 1), TODAY)).isEqualTo(1270);
    }

    @Test
    void 오늘_보호가_끝나면_지원은_5년_뒤에_끝난다() {
        // 윤년(2028)이 한 번 껴서 1825가 아니라 1826일이다.
        assertThat(OnboardingCalculator.dDay(TODAY, TODAY)).isEqualTo(1826);
    }

    @Test
    void 지원_기간이_지났으면_dDay가_음수다() {
        // 2015년에 보호가 끝났으면 지원은 2020년에 종료됐다. 0으로 깎으면 "끝났다"가 화면에서 사라진다.
        assertThat(OnboardingCalculator.dDay(LocalDate.of(2015, 1, 1), TODAY)).isNegative();
        assertThat(OnboardingCalculator.remainingMonths(LocalDate.of(2015, 1, 1), TODAY)).isNegative();
    }

    @Test
    void 남은_개월_수는_지원_종료일까지의_개월이다() {
        // 2026-09-08 → 2031-09-08 = 정확히 60개월
        assertThat(OnboardingCalculator.remainingMonths(LocalDate.of(2026, 9, 8), TODAY))
                .isEqualTo(60);
    }

    @Test
    void 보호종료일이_미래면_보호중이다() {
        assertThat(OnboardingCalculator.protectionStatus(TODAY.plusDays(1), TODAY))
                .isEqualTo(ProtectionStatus.IN_PROTECTION);
    }

    @Test
    void 보호종료일_당일은_종료로_본다() {
        // 그날부터 지원 기간이 시작되므로 당일을 보호중으로 두면 D-day 기준과 어긋난다.
        assertThat(OnboardingCalculator.protectionStatus(TODAY, TODAY))
                .isEqualTo(ProtectionStatus.DISCHARGED);
    }

    @Test
    void 보호종료일이_과거면_종료다() {
        assertThat(OnboardingCalculator.protectionStatus(TODAY.minusDays(1), TODAY))
                .isEqualTo(ProtectionStatus.DISCHARGED);
    }
}
