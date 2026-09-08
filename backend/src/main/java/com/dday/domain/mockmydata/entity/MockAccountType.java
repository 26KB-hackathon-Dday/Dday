package com.dday.domain.mockmydata.entity;

/**
 * Mock 계좌의 유형.
 *
 * <p>{@code com.dday.domain.mydata.entity.AccountType}과 값이 같지만 <b>일부러 따로 둔다.</b>
 * 이쪽은 외부 금융기관이 내려주는 값을 흉내내는 것이라, 서비스 내부 분류가 바뀌었다고
 * 같이 바뀌면 안 된다. 실제 마이데이터 API로 갈아끼울 때 이 enum만 버리면 된다.
 */
public enum MockAccountType {

    DEPOSIT,
    SAVINGS,
    INVESTMENT,
    LOAN,
    ETC
}
