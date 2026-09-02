package com.dday.domain.welfare.entity;

/**
 * 복지서비스를 운영하는 주체 구분.
 *
 * <p>이번 수집기는 {@link #CENTRAL}만 다룬다. {@link #LOCAL}(지자체)은 응답 스키마와
 * 신선도 판정 방식이 달라(lastModYmd 유무) 별도 잡으로 분리한다 — docs/welfare-collector.md §8.
 */
public enum AgencyType {
    CENTRAL,
    LOCAL
}
