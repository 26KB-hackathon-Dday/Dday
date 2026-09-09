package com.dday.domain.budget.controller;

import com.dday.domain.budget.dto.BudgetSuccessCode;
import com.dday.domain.budget.dto.request.MonthlyBudgetConfirmRequest;
import com.dday.domain.budget.dto.response.MonthlyBudgetConfirmResponse;
import com.dday.domain.budget.service.BudgetService;
import com.dday.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService budgetService;

    @PostMapping("/current/confirm")
    public ResponseEntity<ApiResponse<MonthlyBudgetConfirmResponse>> confirmCurrent(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MonthlyBudgetConfirmRequest request) {
        return ApiResponse.of(BudgetSuccessCode.MONTHLY_BUDGET_CONFIRMED,
                budgetService.confirmCurrent(userId, request));
    }
}
