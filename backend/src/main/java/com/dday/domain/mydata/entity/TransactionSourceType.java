package com.dday.domain.mydata.entity;

/**
 * 거래가 계좌에서 왔는지 카드에서 왔는지.
 *
 * <p>{@code financial_transaction}은 둘을 한 테이블에 담는다. 어느 쪽인지에 따라
 * {@code account_id}와 {@code card_id} 중 정확히 하나만 채워진다.
 */
public enum TransactionSourceType {

    ACCOUNT,
    CARD
}
