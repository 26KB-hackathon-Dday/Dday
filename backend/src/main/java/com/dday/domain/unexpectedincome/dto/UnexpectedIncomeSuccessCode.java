package com.dday.domain.unexpectedincome.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UnexpectedIncomeSuccessCode implements SuccessCode {

    PENDING_INCOME_FOUND(
            HttpStatus.OK,
            "새로 들어온 돈 조회에 성공했습니다."
    ),

    INCOME_EXCLUDED(
            HttpStatus.OK,
            "이번 달 예산에 포함하지 않았습니다."
    ),

    INCOME_ADDED_TO_BUDGET(
            HttpStatus.OK,
            "새로 들어온 돈을 이번 달 예산에 추가했습니다."
    );

    private final HttpStatus status;
    private final String message;
}