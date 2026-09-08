package com.dday.domain.welfare.entity;

/**
 * 유저가 이 제도를 받고 있는지 ({@code user_program_status.receiving_status}).
 *
 * <p>자동탐지는 {@link #LIKELY_RECEIVING}으로 <b>제안만</b> 하고, 최종 확정은 사용자의
 * PATCH 호출로만 {@link #RECEIVING}/{@link #NOT_RECEIVING}이 된다.
 */
public enum ReceivingStatus {
    RECEIVING,
    NOT_RECEIVING,
    LIKELY_RECEIVING,
    UNKNOWN
}
