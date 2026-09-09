package com.dday.domain.unexpectedincome.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UnexpectedIncomeErrorCode
        implements ErrorCode {

    INCOME_TRANSACTION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "입금 거래를 찾을 수 없습니다."
    ),

    INCOME_ALREADY_PROCESSED(
            HttpStatus.BAD_REQUEST,
            "이미 처리한 입금입니다."
    ),

    INVALID_ADD_AMOUNT(
            HttpStatus.BAD_REQUEST,
            "예산에 추가할 금액을 확인해주세요."
    ),

    INVALID_ALLOCATION(
            HttpStatus.BAD_REQUEST,
            "네 개 포켓의 배분 금액을 모두 확인해주세요."
    ),

    ALLOCATION_AMOUNT_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "포켓 배분 금액의 합계가 추가 금액과 일치하지 않습니다."
    ),

    CURRENT_BUDGET_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "해당 월의 확정 예산을 찾을 수 없습니다."
    ),

    MONTHLY_POCKET_BUDGET_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "해당 월의 포켓 예산을 찾을 수 없습니다."
    );

    private final HttpStatus status;

    private final String message;
}