package com.dday.domain.asset.entity;

/**
 * 거래 체결 상태.
 *
 * <p>{@link #CORRECTED}는 금융기관이 체결 내역을 정정한 경우다. 원본을 지우고 다시 넣지 않고
 * 상태를 바꾸는 이유는, 재동기화 때 같은 원본 거래 ID가 다시 내려오면 그때 또 지웠다 넣는
 * 일이 반복되기 때문이다.
 */
public enum ExecutionStatus {

    EXECUTED,
    CANCELED,
    CORRECTED
}
