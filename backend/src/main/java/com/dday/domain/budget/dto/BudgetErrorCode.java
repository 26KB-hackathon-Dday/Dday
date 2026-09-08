package com.dday.domain.budget.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BudgetErrorCode implements ErrorCode {

    MONTHLY_BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 월의 예산을 찾을 수 없습니다."),
    MONTHLY_POCKET_BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 월의 포켓 예산을 찾을 수 없습니다."),
    MONTHLY_BUDGET_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 월의 예산이 이미 확정되었습니다."),
    POCKETS_NOT_INITIALIZED(HttpStatus.BAD_REQUEST, "기본 포켓 네 개를 먼저 생성해주세요."),
    INVALID_POCKET_BUDGETS(HttpStatus.BAD_REQUEST, "포켓별 예산을 올바르게 입력해주세요."),
    BUDGET_TOTAL_MISMATCH(HttpStatus.BAD_REQUEST, "포켓별 예산 합계가 총 예산과 일치해야 합니다.");

    private final HttpStatus status;
    private final String message;
}
