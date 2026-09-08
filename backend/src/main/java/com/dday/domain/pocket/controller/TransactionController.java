package com.dday.domain.pocket.controller;

import com.dday.domain.pocket.dto.PocketSuccessCode;
import com.dday.domain.pocket.dto.response.TransactionDetailResponse;
import com.dday.domain.pocket.service.TransactionQueryService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
}
