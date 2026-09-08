package com.dday.domain.mydata.entity;

/** 연동 계좌의 유형. {@link #INVESTMENT}·{@link #SAVINGS} 계좌의 거래가 미래자산 실적으로 잡힌다. */
public enum AccountType {

    DEPOSIT,
    SAVINGS,
    INVESTMENT,
    LOAN,
    ETC
}
