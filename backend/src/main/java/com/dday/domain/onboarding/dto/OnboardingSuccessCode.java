package com.dday.domain.onboarding.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OnboardingSuccessCode implements SuccessCode {

    ONBOARDING_PROGRESS_FOUND(HttpStatus.OK, "온보딩 진행 상태를 조회했습니다."),
    PROTECTION_DATE_SAVED(HttpStatus.OK, "보호종료일이 저장되었습니다."),
    REGION_SAVED(HttpStatus.OK, "거주지역이 저장되었습니다."),
    HOUSING_TYPE_SAVED(HttpStatus.OK, "주거형태가 저장되었습니다."),
    HOUSING_COST_SAVED(HttpStatus.OK, "주거비가 저장되었습니다."),
    INCOMES_FOUND(HttpStatus.OK, "정기수입을 조회했습니다."),
    INCOME_CREATED(HttpStatus.CREATED, "정기수입이 추가되었습니다."),
    INCOME_UPDATED(HttpStatus.OK, "정기수입이 수정되었습니다."),
    INCOME_DELETED(HttpStatus.OK, "삭제되었습니다."),
    ASSETS_SAVED(HttpStatus.OK, "자산 정보가 저장되었습니다."),
    ONBOARDING_COMPLETED(HttpStatus.OK, "온보딩이 완료되었습니다.");

    private final HttpStatus status;
    private final String message;
}
