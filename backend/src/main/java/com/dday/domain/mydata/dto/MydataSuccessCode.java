package com.dday.domain.mydata.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MydataSuccessCode implements SuccessCode {

    MYDATA_SYNCED(HttpStatus.OK, "MyData 정보를 동기화했습니다.");

    private final HttpStatus status;
    private final String message;
}
