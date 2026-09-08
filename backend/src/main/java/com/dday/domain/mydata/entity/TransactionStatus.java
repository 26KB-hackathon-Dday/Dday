package com.dday.domain.mydata.entity;

/**
 * 거래의 유효 상태.
 *
 * <p>취소·환불된 거래는 지우지 않고 상태만 바꾼다. 원본 거래와 취소 거래가 둘 다 남아야
 * 마이데이터를 다시 당겼을 때 같은 결과가 나온다. 소진액 집계에서는 {@link #NORMAL}만 센다.
 */
public enum TransactionStatus {

    NORMAL,
    CANCELED,
    REFUNDED
}
