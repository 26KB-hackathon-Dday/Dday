package com.dday.domain.budget.controller;

import com.dday.domain.budget.dto.BudgetSuccessCode;
import com.dday.domain.budget.dto.request.MonthlyBudgetConfirmRequest;
import com.dday.domain.budget.dto.response.BudgetRecommendationResponse;
import com.dday.domain.budget.dto.response.MonthlyBudgetConfirmResponse;
import com.dday.domain.budget.service.BudgetService;
import com.dday.global.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "월 예산")
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetService budgetService;

    @Operation(
            summary = "온보딩 기반 이번 달 추천 예산 조회",
            description = """
                    온보딩에서 입력한 사용자 데이터를 이용해
                    최초 포켓 예산을 추천합니다.

                    사용 데이터:
                    - 등록한 정기수입 합계
                    - 월세
                    - 관리비
                    - 현재 모아둔 자산

                    이 API를 호출해도 월 예산은 아직 확정되지 않습니다.
                    사용자가 추천값을 확인하거나 조정한 뒤
                    예산 확정 API를 호출할 때 실제 DB에 저장됩니다.
                    """
    )
    @GetMapping("/current/recommendation")
    public ResponseEntity<ApiResponse<BudgetRecommendationResponse>> recommendCurrent(
            @AuthenticationPrincipal Long userId
    ) {

        return ApiResponse.of(
                BudgetSuccessCode.BUDGET_RECOMMENDATION_FOUND,
                budgetService.recommendCurrent(
                        userId
                )
        );
    }

    @Operation(
            summary = "이번 달 예산 확정"
    )
    @PostMapping("/current/confirm")
    public ResponseEntity<ApiResponse<MonthlyBudgetConfirmResponse>> confirmCurrent(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MonthlyBudgetConfirmRequest request
    ) {

        return ApiResponse.of(
                BudgetSuccessCode.MONTHLY_BUDGET_CONFIRMED,
                budgetService.confirmCurrent(
                        userId,
                        request
                )
        );
    }
}