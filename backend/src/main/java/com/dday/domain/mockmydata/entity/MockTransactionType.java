package com.dday.domain.mockmydata.entity;

/**
 * Mock 계좌거래의 원본 유형.
 *
 * <p><b>서비스 쪽 {@code TransactionType}과 값이 다르다.</b> 실제 금융기관은 이체를 그냥
 * {@link #TRANSFER}로 내려줄 뿐, 그게 본인 계좌 간 이동인지 남에게 보낸 건지 모른다.
 * {@code SELF_TRANSFER} 판정은 상대 계좌가 내 것인지 아는 서비스 쪽에서 한다 —
 * 여기에 그 값을 두면 Mock이 실제 API보다 똑똑해져서, 진짜로 붙였을 때 판정 로직이
 * 없다는 걸 그제서야 알게 된다.
 */
public enum MockTransactionType {

    INCOME,
    EXPENSE,
    TRANSFER,
    OTHER
}
