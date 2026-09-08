package com.dday.domain.welfare.dto.response;

/**
 * 신청 가능 여부. 저장되는 값이 아니라 마감일·상시접수 여부에서 <b>파생</b>된다.
 * 화면 뱃지(신청가능 / 마감)에 그대로 쓴다.
 */
public enum ProgramStatus {
    OPEN,
    CLOSED
}
