package com.dday.domain.pocket.controller;

import com.dday.domain.pocket.dto.PocketSuccessCode;
import com.dday.domain.pocket.dto.response.TransactionDetailResponse;
import com.dday.domain.pocket.dto.request.TransactionClassificationRequest;
import com.dday.domain.pocket.dto.response.AutoClassificationResponse;
import com.dday.domain.pocket.dto.response.TransactionClassificationResponse;
import com.dday.domain.pocket.service.TransactionQueryService;
import com.dday.domain.pocket.service.TransactionClassificationService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "거래")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionQueryService transactionQueryService;
    private final TransactionClassificationService transactionClassificationService;

    @Operation(summary = "미분류 거래 자동 분류", description = """
            로그인 회원의 미분류 정상 소비 거래만 대상으로 한다.
            사용자 가맹점 규칙을 먼저 적용하고, 일치하는 규칙이 없으면 자유 포켓으로 분류한다.
            수동 분류 거래와 수입·본인 이체·취소·환불 거래는 변경하지 않는다.
            """)
    @PostMapping("/classify")
    public ResponseEntity<ApiResponse<AutoClassificationResponse>> classifyUnclassified(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(PocketSuccessCode.TRANSACTIONS_CLASSIFIED,
                transactionClassificationService.classifyUnclassified(userId));
    }

    @Operation(summary = "거래 상세 조회", description = """
            로그인한 회원이 소유한 계좌 또는 카드의 거래만 조회한다.
            다른 회원의 거래 ID도 존재하지 않는 거래와 동일하게 처리한다.
            """)
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionDetailResponse>> findById(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId) {
        return ApiResponse.of(PocketSuccessCode.TRANSACTION_FOUND,
                transactionQueryService.findById(userId, transactionId));
    }

    @Operation(summary = "거래 분류 직접 변경", description = """
            정상 상태의 소비 거래를 필수 또는 자유 포켓과 해당 유형의 활성 카테고리로 변경한다.
            applyFutureRule이 true이면 같은 가맹점의 이후 거래에 적용할 사용자 규칙도 저장한다.
            """)
    @PatchMapping("/{transactionId}/classification")
    public ResponseEntity<ApiResponse<TransactionClassificationResponse>> classifyManually(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId,
            @Valid @RequestBody TransactionClassificationRequest request) {
        return ApiResponse.of(PocketSuccessCode.TRANSACTION_CLASSIFICATION_CHANGED,
                transactionClassificationService.classifyManually(userId, transactionId, request));
    }
}
