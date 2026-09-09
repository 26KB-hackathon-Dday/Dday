package com.dday.domain.unexpectedincome.dto;

public enum UnexpectedIncomeType {

    /**
     * 일반 신규 입금.
     */
    NEW_INCOME,

    /**
     * 등록된 고정수입과
     * 입금 시기와 금액이 모두 비슷함.
     *
     * 자동 처리하지 않고 사용자에게 확인받는다.
     */
    RECURRING_LIKELY,

    /**
     * 등록된 고정수입과 시기는 비슷하지만
     * 등록 금액보다 의미 있게 많이 들어옴.
     */
    RECURRING_OVER
}