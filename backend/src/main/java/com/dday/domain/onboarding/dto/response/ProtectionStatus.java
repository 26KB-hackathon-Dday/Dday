package com.dday.domain.onboarding.dto.response;

/** 보호 중인지 보호가 끝났는지. 화면이 문구를 가르는 값이라 응답에 그대로 나간다. */
public enum ProtectionStatus {

    /** 아직 보호 중 (보호종료일이 미래) */
    IN_PROTECTION,

    /** 보호 종료 (보호종료일이 오늘이거나 과거) */
    DISCHARGED
}
