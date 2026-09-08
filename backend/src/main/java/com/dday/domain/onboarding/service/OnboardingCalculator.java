package com.dday.domain.onboarding.service;

import com.dday.domain.onboarding.dto.response.ProtectionStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 보호종료일에서 지원 종료 시점과 D-day를 계산한다.
 *
 * <p>보호종료 후 <b>5년</b>이 자립 지원 기간이다. 이 상수와 계산식이 여러 API
 * (보호종료일 저장, 온보딩 완료)에 흩어지면 한쪽만 고쳤을 때 화면마다 D-day가 달라진다.
 *
 * <p>{@code today}를 인자로 받는다 — {@code LocalDate.now()}를 안에서 부르면
 * 날짜 경계 케이스를 테스트할 수 없다.
 */
public final class OnboardingCalculator {

    /** 자립 지원 기간(년). 제도가 바뀌면 여기만 고친다. */
    private static final int SUPPORT_YEARS = 5;

    private OnboardingCalculator() {
    }

    /** 지원 종료일 = 보호종료일 + 5년. */
    public static LocalDate supportEndDate(LocalDate protectionEndDate) {
        return protectionEndDate.plusYears(SUPPORT_YEARS);
    }

    /**
     * 지원 종료까지 남은 일수.
     *
     * <p><b>이미 지났으면 음수가 된다.</b> 0으로 깎지 않는 건, 지원이 끝난 회원에게
     * "D-0"을 계속 보여주면 끝났다는 사실이 화면에서 사라지기 때문이다.
     * 어떻게 표기할지는 프론트가 정한다.
     */
    public static long dDay(LocalDate protectionEndDate, LocalDate today) {
        return ChronoUnit.DAYS.between(today, supportEndDate(protectionEndDate));
    }

    /** 지원 종료까지 남은 개월 수. 일수와 같은 이유로 음수가 될 수 있다. */
    public static long remainingMonths(LocalDate protectionEndDate, LocalDate today) {
        return ChronoUnit.MONTHS.between(today, supportEndDate(protectionEndDate));
    }

    /**
     * 보호 중인지 종료했는지.
     *
     * <p>보호종료일 <b>당일은 종료로 본다</b>. 그날부터 지원 기간이 시작되므로
     * 당일을 보호 중으로 두면 D-day 기준과 어긋난다.
     */
    public static ProtectionStatus protectionStatus(LocalDate protectionEndDate, LocalDate today) {
        return protectionEndDate.isAfter(today)
                ? ProtectionStatus.IN_PROTECTION
                : ProtectionStatus.DISCHARGED;
    }
}
