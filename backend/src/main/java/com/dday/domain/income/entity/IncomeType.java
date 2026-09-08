package com.dday.domain.income.entity;

/**
 * 정기수입의 종류.
 *
 * <p>{@code budget}의 {@code FactorType} 중 <b>수입 항목만</b> 추린 것이다. 그쪽 enum을 그대로
 * 쓰지 않는 건 월세·통신비 같은 지출 값이 섞여 있어서다 — 정기수입에 {@code RENT}를 넣을 수
 * 있게 되면 합계가 조용히 틀어진다.
 *
 * <p>예산 초안을 만들 때 여기 값을 {@code FactorType}으로 옮겨 담는다. 새 항목을 더하면
 * 양쪽을 같이 봐야 한다.
 */
public enum IncomeType {

    /** 급여 */
    SALARY,

    /** 자립수당 등 각종 수당 */
    ALLOWANCE,

    /** 자립정착금 */
    SETTLEMENT_FUND,

    /** 디딤씨앗통장 */
    DIDIM_SEED,

    ETC
}
