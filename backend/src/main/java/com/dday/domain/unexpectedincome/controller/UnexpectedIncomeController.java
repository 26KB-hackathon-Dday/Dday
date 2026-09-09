package com.dday.domain.unexpectedincome.controller;

import com.dday.domain.unexpectedincome.dto.UnexpectedIncomeSuccessCode;
import com.dday.domain.unexpectedincome.dto.request.UnexpectedIncomeAddRequest;
import com.dday.domain.unexpectedincome.dto.response.UnexpectedIncomeResponse;
import com.dday.domain.unexpectedincome.service.UnexpectedIncomeService;
import com.dday.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/unexpected-incomes")
public class UnexpectedIncomeController {

    private final UnexpectedIncomeService unexpectedIncomeService;

    /**
     * 아직 사용자가 처리하지 않은 신규 입금 1건 조회
     *
     * 응답 type:
     * - NEW_INCOME
     * - RECURRING_LIKELY
     * - RECURRING_OVER
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<UnexpectedIncomeResponse>> findPending(
            @AuthenticationPrincipal Long userId
    ) {
        UnexpectedIncomeResponse response =
                unexpectedIncomeService.findPending(
                        userId
                );

        return ApiResponse.of(
                UnexpectedIncomeSuccessCode.PENDING_INCOME_FOUND,
                response
        );
    }

    /**
     * 이번 달 예산에 포함하지 않기
     */
    @PatchMapping("/{transactionId}/exclude")
    public ResponseEntity<ApiResponse<Void>> exclude(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId
    ) {
        unexpectedIncomeService.exclude(
                userId,
                transactionId
        );

        return ApiResponse.of(
                UnexpectedIncomeSuccessCode.INCOME_EXCLUDED
        );
    }

    /**
     * 신규 입금의 일부 또는 전부를
     * 이번 달 포켓 예산에 추가
     */
    @PatchMapping("/{transactionId}/add-to-budget")
    public ResponseEntity<ApiResponse<Void>> addToBudget(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId,
            @RequestBody UnexpectedIncomeAddRequest request
    ) {
        unexpectedIncomeService.addToBudget(
                userId,
                transactionId,
                request
        );

        return ApiResponse.of(
                UnexpectedIncomeSuccessCode.INCOME_ADDED_TO_BUDGET
        );
    }
}