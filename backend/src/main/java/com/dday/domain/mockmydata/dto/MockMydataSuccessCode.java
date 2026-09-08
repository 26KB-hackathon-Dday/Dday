package com.dday.domain.mockmydata.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MockMydataSuccessCode implements SuccessCode {

    ACCOUNTS_FOUND(HttpStatus.OK, "Mock MyData 계좌 목록을 조회했습니다."),
    CARDS_FOUND(HttpStatus.OK, "Mock MyData 카드 목록을 조회했습니다."),
    ACCOUNT_TRANSACTIONS_FOUND(HttpStatus.OK, "Mock MyData 계좌 거래를 조회했습니다."),
    CARD_TRANSACTIONS_FOUND(HttpStatus.OK, "Mock MyData 카드 거래를 조회했습니다.");

    private final HttpStatus status;
    private final String message;
}
