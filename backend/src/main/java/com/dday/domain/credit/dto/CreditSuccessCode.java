package com.dday.domain.credit.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CreditSuccessCode implements SuccessCode {

    CREDIT_SCORES_FOUND(HttpStatus.OK, "최근 신용점수를 조회했습니다."),
    EXPECTED_RATES_FOUND(HttpStatus.OK, "예상 금리를 조회했습니다.");

    private final HttpStatus status;
    private final String message;
}
