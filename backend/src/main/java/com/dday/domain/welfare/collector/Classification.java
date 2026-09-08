package com.dday.domain.welfare.collector;

import com.dday.domain.welfare.entity.YouthStatus;

/**
 * {@link YouthClassifier} 판정 결과.
 *
 * @param disposition 이 후보를 어떻게 처리할지
 * @param status      저장될 상태. {@link Disposition#STORED}일 때만 non-null
 * @param score       가중 스코어 합계. {@code STRONG_YOUTH}거나 저장 안 하면 null일 수 있다
 * @param trace       판정 흔적 (예: {@code R2+2,R3+3=5})
 */
public record Classification(Disposition disposition, YouthStatus status, Integer score, String trace) {

    public enum Disposition {
        /** 사전필터 탈락. 저장하지 않는다. */
        DISCARDED,
        /** 스코어 < 0 ("자동 제외"). 저장하지 않는다 — 사용자 결정. */
        REJECTED,
        /** {@code welfare_program}에 upsert. */
        STORED
    }

    public boolean stored() {
        return disposition == Disposition.STORED;
    }

    static Classification discarded() {
        return new Classification(Disposition.DISCARDED, null, null, "prefilter");
    }

    static Classification rejected(int score, String trace) {
        return new Classification(Disposition.REJECTED, null, score, trace);
    }

    static Classification strongYouth() {
        return new Classification(Disposition.STORED, YouthStatus.STRONG_YOUTH, null, "R1");
    }

    static Classification scored(YouthStatus status, int score, String trace) {
        return new Classification(Disposition.STORED, status, score, trace);
    }
}
