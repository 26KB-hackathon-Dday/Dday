package com.dday.domain.budget.entity;

/** 예산 초안을 만들 때 더하거나 뺀 항목의 종류. 수입 항목이 앞, 고정지출 항목이 뒤다. */
public enum FactorType {

    /** 급여 */
    SALARY,

    /** 각종 수당 */
    ALLOWANCE,

    /** 자립정착금 */
    SETTLEMENT_FUND,

    /** 디딤씨앗통장 */
    DIDIM_SEED,

    /** 월세 */
    RENT,

    /** 공과금 */
    UTILITY,

    /** 통신비 */
    TELECOM,

    /** 보험료 */
    INSURANCE,

    /** 기타 고정지출 */
    FIXED_EXPENSE,

    ETC
}
