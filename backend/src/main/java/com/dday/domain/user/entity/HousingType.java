package com.dday.domain.user.entity;

/**
 * 주거 형태. 지원금 매칭 조건이자 예산 초안에서 월세 항목을 잡을지 가르는 값이다.
 *
 * <p>{@link #LH_JEONSE}·{@link #SELF_RELIANCE_HOUSE}처럼 보증금만 있고 월세가 없는 형태가 있어
 * "월세 얼마"만 물어서는 예산이 안 잡힌다.
 */
public enum HousingType {

    /** LH 전세임대 */
    LH_JEONSE,

    /** 월세 */
    MONTHLY,

    /** 자립생활관 */
    SELF_RELIANCE_HOUSE,

    /** 기숙사 */
    DORM,

    /** 원가정·친척집 */
    FAMILY,

    ETC
}
