package com.dday.domain.budget.dto;

import com.dday.global.common.code.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BudgetSuccessCode
        implements SuccessCode {

    BUDGET_RECOMMENDATION_FOUND(
            HttpStatus.OK,
            "온보딩 정보를 기준으로 이번 달 추천 예산을 조회했습니다."
    ),

    MONTHLY_BUDGET_CONFIRMED(
            HttpStatus.CREATED,
            "이번 달 예산을 확정했습니다."
    );

    private final HttpStatus status;

    private final String message;
}