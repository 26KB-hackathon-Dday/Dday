package com.dday.domain.budgetadjustment.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BudgetAdjustmentErrorCode implements ErrorCode {

    MONTHLY_BUDGET_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "이번 달 확정 예산을 찾을 수 없습니다."
    ),

    POCKET_BUDGET_NOT_READY(
            HttpStatus.BAD_REQUEST,
            "이번 달 포켓 예산이 모두 준비되지 않았습니다."
    ),

    INVALID_POCKET_COUNT(
            HttpStatus.BAD_REQUEST,
            "네 개의 포켓 예산을 모두 입력해 주세요."
    ),

    DUPLICATED_POCKET_TYPE(
            HttpStatus.BAD_REQUEST,
            "같은 포켓이 중복으로 입력되었습니다."
    ),

    INVALID_TOTAL_BUDGET(
            HttpStatus.BAD_REQUEST,
            "총 예산은 0원보다 커야 합니다."
    ),

    BUDGET_SUM_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "포켓 예산 합계가 총 예산과 일치하지 않습니다."
    ),

    BELOW_SPENT_AMOUNT(
            HttpStatus.BAD_REQUEST,
            "이미 사용한 금액보다 포켓 예산을 낮출 수 없습니다."
    );

    private final HttpStatus status;
    private final String message;
}