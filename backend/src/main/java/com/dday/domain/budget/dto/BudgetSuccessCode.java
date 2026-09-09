package com.dday.domain.budget.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BudgetSuccessCode implements SuccessCode {
    MONTHLY_BUDGET_CONFIRMED(HttpStatus.CREATED, "이번 달 예산을 확정했습니다.");

    private final HttpStatus status;
    private final String message;
}
