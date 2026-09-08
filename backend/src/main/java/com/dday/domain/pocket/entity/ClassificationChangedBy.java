package com.dday.domain.pocket.entity;

/** 거래 분류를 바꾼 주체. 사용자가 직접 고친 건 자동분류가 덮어쓰면 안 되므로 구분해서 남긴다. */
public enum ClassificationChangedBy {

    USER,
    SYSTEM
}
