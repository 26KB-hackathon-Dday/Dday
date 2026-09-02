package com.dday.domain.welfare.entity;

/**
 * {@code welfare_program} 행이 어디서 왔는지.
 *
 * <p>지금은 수집 배치가 만든 후보({@link #API_CANDIDATE})만 있다. 관리자가 직접 등록하거나
 * 검토를 통과시킨 행을 다른 값으로 구분하게 될 때를 위해 enum으로 둔다.
 */
public enum ProgramSource {
    /** 수집 배치가 외부 API에서 긁어와 룰로 통과시킨 후보. 아직 사람 검토 전이다. */
    API_CANDIDATE
}
