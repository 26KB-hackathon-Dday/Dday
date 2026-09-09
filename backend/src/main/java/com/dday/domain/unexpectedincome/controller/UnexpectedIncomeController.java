package com.dday.domain.unexpectedincome.controller;

import com.dday.domain.unexpectedincome.dto.UnexpectedIncomeSuccessCode;
import com.dday.domain.unexpectedincome.dto.request.UnexpectedIncomeAddRequest;
import com.dday.domain.unexpectedincome.dto.response.PendingUnexpectedIncomeResponse;
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
     * 아직 사용자가 처리하지 않은 신규 입금을 모두 조회한다.
     *
     * 각 입금은 백엔드에서
     *
     * - NEW_INCOME
     * - RECURRING_LIKELY
     * - RECURRING_OVER
     *
     * 중 하나로 분류된다.
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<PendingUnexpectedIncomeResponse>> findPending(
            @AuthenticationPrincipal Long userId
    ) {
        PendingUnexpectedIncomeResponse response =
                unexpectedIncomeService.findPending(userId);

        return ApiResponse.of(
                UnexpectedIncomeSuccessCode.PENDING_INCOME_FOUND,
                response
        );
    }

    /**
     * 이번 달 예산에 포함하지 않기.
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
     * 이번 달 포켓 예산에 추가한다.
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