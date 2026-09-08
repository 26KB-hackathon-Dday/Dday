package com.dday.domain.welfare.entity;

/** 지원 형태. Notion 명세 §2.1 {@code supportType}. */
public enum SupportType {

    /** 현금 지급. */
    CASH,

    /** 대출 (이자지원·상환유예 포함). */
    LOAN,

    /** 바우처·이용권. */
    VOUCHER,

    /** 서비스 제공 (상담·돌봄 등 비현금). */
    SERVICE
}
