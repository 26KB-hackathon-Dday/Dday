package com.dday.domain.onboarding.dto.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 온보딩을 마치고 보여주는 첫 자립계획 요약.
 *
 * <p>{@code monthlyExpense}는 지금은 주거비(월세 + 관리비)뿐이다. 예산 도메인이
 * 초안을 만들기 시작하면 그 지출 합계가 더해진다 ({@code OnboardingService}의 TODO 참고).
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
public class OnboardingCompleteResponse {

    /**
     * 지원 종료까지 남은 일수. 보호종료일을 입력하지 않았으면 null이다.
     *
     * <p>Jackson이 getDDay()를 "dday"로 내보내서 이름을 고정한다.
     */
    @JsonProperty("dDay")
    private final Long dDay;

    private final long monthlyIncome;
    private final long monthlyExpense;

    public static OnboardingCompleteResponse of(Long dDay, long monthlyIncome, long monthlyExpense) {
        return OnboardingCompleteResponse.builder()
                .dDay(dDay)
                .monthlyIncome(monthlyIncome)
                .monthlyExpense(monthlyExpense)
                .build();
    }
}
