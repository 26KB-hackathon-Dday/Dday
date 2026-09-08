package com.dday.domain.mydata.entity;

/**
 * 무엇을 근거로 분류했는지. 선언 순서가 곧 <b>자동분류 우선순위</b>다 — 위에서부터 시도해
 * 맞는 게 나오면 거기서 멈춘다.
 *
 * <p>분류가 틀렸다는 제보가 들어왔을 때 어느 단계를 고쳐야 하는지 이 값 하나로 알 수 있다.
 */
public enum ClassificationSource {

    /** 사용자가 만든 가맹점 규칙 — 항상 최우선 */
    USER_RULE,

    /** 마이데이터가 함께 내려준 업종 분류 */
    MYDATA_CATEGORY,

    /** 가맹점 사업자번호로 맞춘 내부 가맹점 DB */
    MERCHANT_DB,

    /** 가맹점명 키워드 규칙 */
    MERCHANT_NAME_RULE,

    /** 사용자가 이 거래 하나만 직접 지정 */
    MANUAL,

    /** 아무것도 못 맞췄을 때의 기본값 — 자유 포켓으로 보낸다 */
    DEFAULT_FREE
}
