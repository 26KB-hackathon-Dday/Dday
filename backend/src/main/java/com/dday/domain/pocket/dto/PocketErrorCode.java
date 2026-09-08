package com.dday.domain.pocket.dto;

import com.dday.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * ⚠️ 여기 적힌 {@code message}가 에러 문구의 정본이다. 프론트가 화면에 그대로 띄운다.
 */
@Getter
@RequiredArgsConstructor
public enum PocketErrorCode implements ErrorCode {

    POCKET_NOT_FOUND(HttpStatus.NOT_FOUND, "포켓을 찾을 수 없습니다."),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "거래 정보를 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    INVALID_MONTH(HttpStatus.BAD_REQUEST, "month는 yyyy-MM 형식이어야 합니다."),
    INVALID_POCKET_TYPE(HttpStatus.BAD_REQUEST, "소비 거래는 필수 또는 자유 포켓만 조회할 수 있습니다."),
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "page는 0 이상, size는 1 이상 100 이하여야 합니다."),
    INVALID_CLASSIFICATION_TARGET(HttpStatus.BAD_REQUEST, "정상 상태의 소비 거래만 분류할 수 있습니다."),
    CATEGORY_POCKET_MISMATCH(HttpStatus.BAD_REQUEST, "선택한 카테고리는 해당 포켓에 사용할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
