package com.dday.domain.mydata.entity;

/**
 * 서비스가 판단한 거래 성격.
 *
 * <p>{@link #SELF_TRANSFER}는 마이데이터 원본에 없는 값이다. 원본은 그냥 '이체'로 내려주는데,
 * 본인 계좌끼리 옮긴 돈을 소비로 세면 지출이 두 배로 부풀기 때문에 수집 단계에서
 * 상대 계좌가 본인 것인지 확인해 이 값으로 바꿔 저장한다.
 */
public enum TransactionType {

    /** 입금 */
    INCOME,

    /** 일반 소비 */
    EXPENSE,

    /** 본인 계좌 간 이체 — 예산 집계에서 제외한다 */
    SELF_TRANSFER,

    OTHER
}
