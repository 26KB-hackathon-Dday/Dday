package com.dday.domain.welfare.entity;

/**
 * {@code receiving_status}를 무엇이 정했는지 ({@code user_program_status.detection_source}).
 * {@code receiving_status == UNKNOWN}이면 {@code null}.
 */
public enum DetectionSource {
    /** 사용자가 직접 체크. */
    USER_INPUT,
    /** 마이데이터 탐지로 제안된 것을 사용자가 확정. */
    AUTO_DETECTED
}
