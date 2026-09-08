package com.dday.domain.welfare.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WelfareSuccessCode implements SuccessCode {

    WELFARE_COLLECT_TRIGGERED(HttpStatus.ACCEPTED, "복지서비스 수집 잡을 실행했습니다."),
    WELFARE_PROGRAMS_FOUND(HttpStatus.OK, "지원제도 목록을 조회했습니다."),
    WELFARE_PROGRAM_FOUND(HttpStatus.OK, "지원제도를 조회했습니다."),
    WELFARE_REVIEW_QUEUE_FOUND(HttpStatus.OK, "리뷰 큐를 조회했습니다.");

    private final HttpStatus status;
    private final String message;
}
