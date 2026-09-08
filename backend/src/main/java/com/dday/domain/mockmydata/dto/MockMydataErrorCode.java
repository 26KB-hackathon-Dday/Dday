package com.dday.domain.mockmydata.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MockMydataErrorCode implements ErrorCode {

    MOCK_MYDATA_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Mock MyData 사용자를 찾을 수 없습니다."),
    MOCK_MYDATA_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Mock MyData 계좌를 찾을 수 없습니다."),
    MOCK_MYDATA_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "Mock MyData 카드를 찾을 수 없습니다."),
    INVALID_TRANSACTION_PERIOD(HttpStatus.BAD_REQUEST, "거래 조회 기간이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
