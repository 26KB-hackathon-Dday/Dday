package com.dday.domain.welfare.entity;

/**
 * 큐레이션 검증 결과. {@code null}이면 아직 검증 전(수집만 되고 품질 스텝을 안 거침).
 *
 * @see com.dday.domain.welfare.collector.validation.WelfareProgramValidator
 */
public enum CurationStatus {

    /** 점검 항목에 걸린 게 없음 — 그대로 노출해도 됨. */
    OK,

    /** 하나 이상 걸림 — 노출은 하되 사람이 확인해야 함. 사유는 {@code curationIssues}. */
    NEEDS_REVIEW
}
