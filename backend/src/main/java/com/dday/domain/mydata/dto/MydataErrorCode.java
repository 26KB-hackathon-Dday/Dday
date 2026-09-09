package com.dday.domain.mydata.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MydataErrorCode implements ErrorCode {

    INVALID_SYNC_PERIOD(HttpStatus.BAD_REQUEST, "동기화 기간이 올바르지 않습니다."),
    MYDATA_CONSENT_REQUIRED(HttpStatus.FORBIDDEN, "유효한 MyData 연결 동의가 필요합니다."),
    MYDATA_CLIENT_FAILURE(HttpStatus.BAD_GATEWAY, "MyData 정보를 불러오지 못했습니다."),
    MYDATA_SYNC_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "MyData 정보를 저장하지 못했습니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "연동된 계좌를 찾을 수 없습니다."),
    INSTITUTION_NOT_CONNECTED(HttpStatus.NOT_FOUND, "연동된 기관을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
