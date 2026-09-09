package com.dday.domain.budgetadjustment.controller;

import com.dday.domain.budgetadjustment.dto.BudgetAdjustmentSuccessCode;
import com.dday.domain.budgetadjustment.dto.request.BudgetAdjustmentRequest;
import com.dday.domain.budgetadjustment.dto.response.BudgetAdjustmentResponse;
import com.dday.domain.budgetadjustment.service.BudgetAdjustmentService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "진행 중 예산 조정")
@RestController
@RequestMapping("/api/budget-adjustments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BudgetAdjustmentController {

    private final BudgetAdjustmentService budgetAdjustmentService;

    @Operation(
            summary = "현재 예산 조정 정보 조회",
            description = """
                    이번 달에 확정된 총 예산과 포켓별 배정액,
                    현재까지 사용한 금액을 조회합니다.

                    각 포켓은 이미 사용한 금액 이하로 줄일 수 없습니다.
                    """
    )
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<BudgetAdjustmentResponse>> findCurrent(
            @AuthenticationPrincipal Long userId
    ) {

        return ApiResponse.of(
                BudgetAdjustmentSuccessCode.CURRENT_BUDGET_FOUND,
                budgetAdjustmentService.findCurrent(userId)
        );
    }

    @Operation(
            summary = "현재 예산 재조정",
            description = """
                    진행 중인 이번 달 총 예산과 네 개 포켓의 배정 금액을 수정합니다.

                    - 네 포켓 합계는 총 예산과 같아야 합니다.
                    - 각 포켓은 이미 사용한 금액보다 낮게 설정할 수 없습니다.
                    - 총 예산 역시 이미 사용한 총액보다 낮게 설정할 수 없습니다.
                    - 총 예산 변경과 포켓 재배분 내역은 변경 이력으로 저장됩니다.
                    """
    )
    @PatchMapping("/current")
    public ResponseEntity<ApiResponse<BudgetAdjustmentResponse>> adjust(
            @AuthenticationPrincipal Long userId,
            @RequestBody BudgetAdjustmentRequest request
    ) {

        return ApiResponse.of(
                BudgetAdjustmentSuccessCode.BUDGET_ADJUSTED,
                budgetAdjustmentService.adjust(
                        userId,
                        request
                )
        );
    }
}