package com.dday.global.exception;

import com.dday.global.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 모든 에러 응답을 여기서 만든다.
 *
 * <p><b>컨트롤러에서 try-catch로 에러 응답을 만들지 않는다.</b> 서비스는 그냥
 * {@link BusinessException}을 던지고, 응답 모양은 이 클래스 하나가 책임진다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 우리가 의도적으로 던진 예외. 스택트레이스를 남기지 않는다 — 버그가 아니라 정상 흐름이다. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.info("BusinessException: {} - {}", code.name(), code.getMessage());
        return ApiResponse.error(code, e.getData());
    }

    /** {@code @Valid} 실패. 각 필드의 검증 어노테이션 message가 그대로 errors에 실린다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException e) {
        List<ApiResponse.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE, errors);
    }

    /** 필수 쿼리 파라미터 누락 / 타입 불일치 / 깨진 JSON — 전부 400 INVALID_INPUT_VALUE로 묶는다. */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception e) {
        log.info("잘못된 요청: {}", e.getMessage());
        return ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ApiResponse.error(CommonErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResource(NoResourceFoundException e) {
        return ApiResponse.error(CommonErrorCode.NOT_FOUND);
    }

    /**
     * 여기까지 온 건 전부 예상 못 한 버그다. 스택트레이스를 남기고,
     * 클라이언트에는 내부 메시지를 노출하지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
}
