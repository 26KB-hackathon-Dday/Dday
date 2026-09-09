package com.dday.domain.budgetadjustment.dto;

import com.dday.global.common.code.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BudgetAdjustmentSuccessCode
        implements SuccessCode {

    BUDGET_ADJUSTMENT_FOUND(
            HttpStatus.OK,
            "이번 달 예산 조정 정보를 조회했습니다."
    ),

    TOTAL_BUDGET_UPDATED(
            HttpStatus.OK,
            "이번 달 총 예산을 수정했습니다."
    ),

    BUDGET_ADJUSTED(
            HttpStatus.OK,
            "이번 달 예산을 조정했습니다."
    );

    private final HttpStatus status;

    private final String message;
}