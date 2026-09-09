package com.dday.domain.budgetadjustment.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BudgetAdjustmentSuccessCode implements SuccessCode {

    CURRENT_BUDGET_FOUND(
            HttpStatus.OK,
            "현재 포켓 예산을 조회했습니다."
    ),

    BUDGET_ADJUSTED(
            HttpStatus.OK,
            "포켓 예산을 변경했습니다."
    );

    private final HttpStatus status;
    private final String message;
}