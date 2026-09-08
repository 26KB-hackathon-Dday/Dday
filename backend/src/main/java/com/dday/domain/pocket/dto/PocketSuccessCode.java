package com.dday.domain.pocket.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PocketSuccessCode implements SuccessCode {

    POCKETS_FOUND(HttpStatus.OK, "포켓 목록을 조회했습니다."),
    POCKET_FOUND(HttpStatus.OK, "포켓을 조회했습니다."),
    POCKETS_INITIALIZED(HttpStatus.OK, "기본 포켓을 초기화했습니다."),
    MONTHLY_POCKETS_FOUND(HttpStatus.OK, "월별 포켓 현황을 조회했습니다."),
    POCKET_TRANSACTIONS_FOUND(HttpStatus.OK, "포켓 거래 목록을 조회했습니다."),
    TRANSACTION_FOUND(HttpStatus.OK, "거래 정보를 조회했습니다."),
    CATEGORIES_FOUND(HttpStatus.OK, "카테고리 목록을 조회했습니다."),
    TRANSACTIONS_CLASSIFIED(HttpStatus.OK, "미분류 거래를 자동 분류했습니다."),
    TRANSACTION_CLASSIFICATION_CHANGED(HttpStatus.OK, "거래 분류를 변경했습니다.");

    private final HttpStatus status;
    private final String message;
}
