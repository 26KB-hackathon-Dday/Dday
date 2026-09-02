package com.dday.global.common.dto;

import com.dday.global.common.code.SuccessCode;
import com.dday.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 모든 API 응답을 감싸는 봉투. 성공·실패 양쪽 다 {@code code}와 {@code message}를 갖는다.
 *
 * <pre>{@code
 * // 성공
 * { "success": true, "code": "MEMBERS_FOUND", "message": "회원 목록을 조회했습니다.", "data": [ ... ] }
 *
 * // 실패 — data는 기본적으로 null
 * { "success": false, "code": "MEMBER_NOT_FOUND", "message": "회원을 찾을 수 없습니다.", "data": null }
 *
 * // 검증 실패 — errors가 추가된다 (이때만 나온다)
 * { "success": false, "code": "INVALID_INPUT_VALUE", "message": "입력값이 올바르지 않습니다.",
 *   "data": null, "errors": [ { "field": "email", "reason": "..." } ] }
 * }</pre>
 *
 * 컨트롤러는 문자열을 직접 쓰지 않고 {@link SuccessCode} / {@link ErrorCode} enum만 넘긴다.
 */
@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;

    /** 실패 응답에서도 키 자체는 유지된다({@code null}로 나간다). */
    private final T data;

    /** 검증 실패일 때만 실린다. 그 외에는 JSON에서 아예 빠진다. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<FieldError> errors;

    // ── 성공 ────────────────────────────────────────────────────────────────

    /** 본문 데이터가 없는 성공 응답. */
    public static ResponseEntity<ApiResponse<Void>> of(SuccessCode code) {
        return ResponseEntity.status(code.getStatus())
                .body(new ApiResponse<>(true, code.name(), code.getMessage(), null, null));
    }

    /** 본문 데이터가 있는 성공 응답. */
    public static <T> ResponseEntity<ApiResponse<T>> of(SuccessCode code, T data) {
        return ResponseEntity.status(code.getStatus())
                .body(new ApiResponse<>(true, code.name(), code.getMessage(), data, null));
    }

    // ── 실패 ────────────────────────────────────────────────────────────────

    public static ResponseEntity<ApiResponse<Object>> error(ErrorCode code) {
        return error(code, null);
    }

    /** 실패 + 부가 데이터. 명세가 요구할 때만 쓴다. */
    public static ResponseEntity<ApiResponse<Object>> error(ErrorCode code, Object data) {
        return ResponseEntity.status(code.getStatus())
                .body(new ApiResponse<>(false, code.name(), code.getMessage(), data, null));
    }

    /** 검증 실패 전용. {@code errors}가 채워진 유일한 경로다. */
    public static ResponseEntity<ApiResponse<Object>> error(ErrorCode code, List<FieldError> errors) {
        return ResponseEntity.status(code.getStatus())
                .body(new ApiResponse<>(false, code.name(), code.getMessage(), null, errors));
    }

    /** 어떤 필드가 왜 검증에 걸렸는지. {@code reason}은 검증 어노테이션의 {@code message} 그대로다. */
    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private final String field;
        private final String reason;
    }
}
