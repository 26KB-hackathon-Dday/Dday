package com.dday.domain.budgetadjustment.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BudgetAdjustmentErrorCode implements ErrorCode {

    CURRENT_BUDGET_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "이번 달 예산을 찾을 수 없습니다."
    ),

    BUDGET_NOT_CONFIRMED(
            HttpStatus.BAD_REQUEST,
            "확정된 예산만 변경할 수 있습니다."
    ),

    MONTHLY_POCKET_BUDGET_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "이번 달 포켓 예산을 찾을 수 없습니다."
    ),

    INVALID_TOTAL_BUDGET(
            HttpStatus.BAD_REQUEST,
            "총 예산을 확인해주세요."
    ),

    INVALID_ALLOCATION(
            HttpStatus.BAD_REQUEST,
            "네 개 포켓의 배분 금액을 모두 확인해주세요."
    ),

    DUPLICATED_POCKET_TYPE(
            HttpStatus.BAD_REQUEST,
            "같은 포켓을 중복해서 배분할 수 없습니다."
    ),

    ALLOCATION_AMOUNT_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "포켓 배분 금액의 합계가 총 예산과 일치하지 않습니다."
    ),

    BUDGET_BELOW_SPENT_AMOUNT(
            HttpStatus.BAD_REQUEST,
            "이미 사용한 금액보다 예산을 낮게 설정할 수 없습니다."
    );

    private final HttpStatus status;
    private final String message;
}