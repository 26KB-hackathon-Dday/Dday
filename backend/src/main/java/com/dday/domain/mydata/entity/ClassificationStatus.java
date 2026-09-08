package com.dday.domain.mydata.entity;

/**
 * 거래가 포켓·카테고리로 분류됐는지.
 *
 * <p>{@link #MANUAL_CLASSIFIED}는 사용자가 직접 고친 것이라 <b>자동분류가 덮어쓰지 않는다.</b>
 * 재동기화 때 사용자의 수정이 조용히 되돌아가는 걸 막는 표시다.
 */
public enum ClassificationStatus {

    AUTO_CLASSIFIED,
    MANUAL_CLASSIFIED,
    UNCLASSIFIED
}
