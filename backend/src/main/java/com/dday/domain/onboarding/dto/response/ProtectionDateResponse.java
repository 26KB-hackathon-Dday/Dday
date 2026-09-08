package com.dday.domain.onboarding.dto.response;

import com.dday.domain.onboarding.service.OnboardingCalculator;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 보호종료일을 저장하고 바로 계산 결과를 돌려준다. 날짜를 넣자마자
 * "지원 종료까지 D-1277"을 보여주는 화면이라 저장과 계산이 한 왕복에 끝나야 한다.
 */
/*
 * 필드만 보고 직렬화한다. Lombok 게터까지 열어두면 @JsonProperty로 이름을 고친 필드가
 * 게터에서 파생된 이름으로 한 번 더 나가 같은 값이 두 개 실린다
 * (dDay + dday, isCompleted + completed).
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@Getter
@Builder
@AllArgsConstructor
public class ProtectionDateResponse {

    private final LocalDate protectionEndDate;

    /** 보호종료일 + 5년. */
    private final LocalDate supportEndDate;

    /** 지원 종료까지 남은 개월 수. 이미 지났으면 음수다. */
    private final long remainingMonths;

    /**
     * 지원 종료까지 남은 일수. 이미 지났으면 음수다.
     *
     * <p>Jackson이 getDDay()를 "dday"로 내보내서 이름을 고정한다.
     */
    @JsonProperty("dDay")
    private final long dDay;

    private final ProtectionStatus protectionStatus;

    public static ProtectionDateResponse of(LocalDate protectionEndDate, LocalDate today) {
        return ProtectionDateResponse.builder()
                .protectionEndDate(protectionEndDate)
                .supportEndDate(OnboardingCalculator.supportEndDate(protectionEndDate))
                .remainingMonths(OnboardingCalculator.remainingMonths(protectionEndDate, today))
                .dDay(OnboardingCalculator.dDay(protectionEndDate, today))
                .protectionStatus(OnboardingCalculator.protectionStatus(protectionEndDate, today))
                .build();
    }
}
