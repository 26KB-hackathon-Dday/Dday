package com.dday.domain.onboarding.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 여기 적힌 {@code message}가 프론트 화면에 그대로 뜬다 (AGENTS.md §6).
 *
 * <p>{@link #INCOME_NOT_FOUND}는 "없음"과 "남의 것"을 <b>구분하지 않는다</b>. 403으로 나누면
 * 남의 수입 id를 찍어보며 어떤 id가 존재하는지 알아낼 수 있다.
 */
@Getter
@RequiredArgsConstructor
public enum OnboardingErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    INCOME_NOT_FOUND(HttpStatus.NOT_FOUND, "정기수입을 찾을 수 없습니다."),
    PROTECTION_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "보호종료일을 먼저 입력해주세요.");

    private final HttpStatus status;
    private final String message;
}
