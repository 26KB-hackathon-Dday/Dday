package com.dday.domain.asset.entity;

/**
 * 투자·저축 거래의 방향.
 *
 * <p>매수/매도는 투자상품, 입금/출금은 적금 계열에 쓴다. 하나로 합치지 않는 이유는
 * 화면 문구가 다르고("3주 매수" vs "10만원 납입"), 수량·단가가 있는지도 다르기 때문이다.
 */
public enum InvestmentActionType {

    BUY,
    SELL,
    DEPOSIT,
    WITHDRAW
}
