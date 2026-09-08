package com.dday.domain.mockmydata.entity;

/** Mock 거래의 상태. 취소·환불도 행을 지우지 않고 상태로 표현한다. */
public enum MockTransactionStatus {

    NORMAL,
    CANCELED,
    REFUNDED
}
