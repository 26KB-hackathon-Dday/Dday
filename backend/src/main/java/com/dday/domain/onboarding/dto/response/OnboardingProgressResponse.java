package com.dday.domain.onboarding.dto.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 온보딩 진행 상태.
 *
 * <p>지금은 완료 여부만 있다 — 단계를 저장하는 컬럼이 없어서 "이어하기"는 지원하지 않는다.
 * 프론트가 스토어로 단계를 들고 있다가 새로고침하면 처음부터 다시 한다.
 * 이어하기가 필요해지면 {@code users.onboarding_step}을 추가하고 여기에 필드를 더한다.
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
public class OnboardingProgressResponse {

    /** Jackson은 boolean 게터의 is 접두사를 떼어 "completed"로 내보낸다. 명세가 isCompleted라 이름을 고정한다. */
    @JsonProperty("isCompleted")
    private final boolean isCompleted;

    public static OnboardingProgressResponse of(boolean completed) {
        return OnboardingProgressResponse.builder().isCompleted(completed).build();
    }
}
