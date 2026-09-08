package com.dday.domain.budget.entity;

/**
 * 초안 산출 근거를 어디서 얻었는지.
 *
 * <p>사용자에게 "이 금액은 어디서 나온 값인가"를 그대로 보여주기 위한 값이다.
 * 마이데이터에서 뽑은 값과 사용자가 직접 적은 값은 신뢰도가 달라서 화면 표기도 다르다.
 */
public enum FactorSourceType {

    /** 연동된 금융거래에서 추출 */
    MYDATA,

    /** 사용자가 온보딩에서 직접 입력 */
    USER_INPUT,

    /** 제도상 정해진 금액 (지원금 등) */
    POLICY,

    /** 시스템이 계산해 채운 값 */
    SYSTEM
}
