package com.dday.domain.user.entity;

/**
 * 자립정착금 수령 여부.
 *
 * <p>{@link #NOT_YET}과 {@link #NONE}을 나눈 이유는 예산에서 다르게 다뤄야 하기 때문이다.
 * 받을 예정이면 들어올 돈으로 잡아 예산 초안에 반영하고, 대상이 아니면 아예 빼야 한다.
 * 하나로 합치면 "언젠가 들어올 돈"을 영영 기다리는 예산이 된다.
 */
public enum SettlementReceived {

    /** 이미 받았다 */
    RECEIVED,

    /** 대상이지만 아직 못 받았다 — 들어올 돈으로 잡는다 */
    NOT_YET,

    /** 대상이 아니다 */
    NONE
}
