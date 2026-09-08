package com.dday.domain.user.entity;

/** 회원 상태. 탈퇴는 행을 지우지 않고 {@link #WITHDRAWN}으로 바꾼다 — 지우면 관련 데이터가 전부 고아가 된다. */
public enum UserStatus {

    ACTIVE,
    WITHDRAWN
}
