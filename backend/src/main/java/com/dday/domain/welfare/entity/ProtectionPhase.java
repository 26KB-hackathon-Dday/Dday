package com.dday.domain.welfare.entity;

/**
 * 제도의 대상이 보호 <b>종료 전</b>인지 <b>후</b>인지 (지원금 매칭 API 명세서 §2.1 {@code protectionPhase}).
 *
 * <p>자립준비청년(보호종료 후) 대상 제도와, 아직 보호 중인 아동 대상 제도(가정위탁·아동복지시설)를
 * 갈라 매칭에서 엉뚱한 대상을 걸러내기 위한 값이다. 명세서 §6.1의 {@code CHILD_POSITIVE} 등급은
 * {@link #PRE_TERMINATION}으로 태깅해 적재하도록 되어 있다.
 *
 * <p>{@code null} = 아직 미분류. 수집 배치가 이 값을 채우는 로직은 후속
 * (명세서 §6.1의 등급 판정을 {@code classificationTier}로 흡수할 때 같이).
 */
public enum ProtectionPhase {

    /** 아직 보호 중인 아동 대상 (가정위탁·아동복지시설). */
    PRE_TERMINATION,

    /** 보호종료 후 = 자립준비청년 대상. */
    POST_TERMINATION,

    /** 보호 중·종료 후 모두 대상. */
    BOTH
}
