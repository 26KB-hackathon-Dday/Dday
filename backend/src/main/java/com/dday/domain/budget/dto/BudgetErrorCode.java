package com.dday.domain.budget.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BudgetErrorCode implements ErrorCode {

    MONTHLY_BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 월의 예산을 찾을 수 없습니다."),
    MONTHLY_POCKET_BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 월의 포켓 예산을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
