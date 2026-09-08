package com.dday.domain.credit.entity;

/**
 * 팁에 딸린 행동 버튼의 종류.
 *
 * <p>지금은 값이 하나뿐이다. 그래도 enum으로 두는 이유는 나중에 "적금 추천" 같은 행동이
 * 붙을 때 컬럼 타입을 바꾸지 않아도 되기 때문이다. 버튼 없는 팁은 {@code null}이다.
 */
public enum TipActionType {

    /** 신용점수 개선 행동으로 보낸다 */
    RAISE_SCORE
}
