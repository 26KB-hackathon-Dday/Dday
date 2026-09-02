package com.dday.domain.welfare.entity;

/**
 * 청년 대상 판별 결과. 판정 룰과 근거는 docs/welfare-api/NOTES.md §7.
 *
 * <p><b>여기 있는 값만 저장된다.</b> 스코어가 음수인 건("자동 제외")과 사전필터에서 걸러진 건은
 * 아예 {@code welfare_program}에 들어오지 않으므로 이 enum에 대응값이 없다.
 */
public enum YouthStatus {

    /** Rule 1(자립준비청년·보호종료 키워드) 매칭. 스코어 계산 없이 즉시 확정. */
    STRONG_YOUTH,

    /** 스코어 ≥ 3. 사람 확인 없이 청년 대상으로 본다. */
    AUTO_APPROVED,

    /** 스코어 0~2. 관리자가 최종 판단해야 하는 회색지대. 관리자는 이 상태만 훑으면 된다. */
    REVIEW_QUEUE
}
