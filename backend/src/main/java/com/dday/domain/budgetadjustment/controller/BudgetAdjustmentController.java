package com.dday.domain.budgetadjustment.controller;

import com.dday.domain.budget.dto.request.TotalBudgetUpdateRequest;
import com.dday.domain.budget.dto.response.TotalBudgetUpdateResponse;
import com.dday.domain.budgetadjustment.dto.BudgetAdjustmentSuccessCode;
import com.dday.domain.budgetadjustment.dto.request.BudgetAdjustmentRequest;
import com.dday.domain.budgetadjustment.dto.response.BudgetAdjustmentResponse;
import com.dday.domain.budgetadjustment.service.BudgetAdjustmentService;
import com.dday.global.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
<<<<<<< HEAD

import jakarta.validation.Valid;

=======
>>>>>>> main
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

<<<<<<< HEAD
                    각 포켓별로 다음 정보를 제공합니다.
                    - 현재 할당 금액
                    - 현재 달성 금액
                    - 재조정 가능한 최소 금액
                    - 남은 금액

                    필수/자유 포켓은 이미 사용한 금액,
                    미래자산 포켓은 이미 투자·저축한 금액보다
                    낮게 조정할 수 없습니다.

                    비상금 포켓은 0원까지 재배분할 수 있습니다.
=======
                    각 포켓은 이미 사용한 금액 이하로 줄일 수 없습니다.
>>>>>>> main
                    """
    )
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<BudgetAdjustmentResponse>> findCurrent(
            @AuthenticationPrincipal Long userId
    ) {

        return ApiResponse.of(
<<<<<<< HEAD
                BudgetAdjustmentSuccessCode.BUDGET_ADJUSTMENT_FOUND,
                budgetAdjustmentService.findCurrent(userId)
        );
    }

    @Operation(
            summary = "이번 달 총 예산만 수정",
            description = """
                    포켓 재배분 전에 이번 달 총 예산만 먼저 수정합니다.

                    최소 총 예산은 다음 금액의 합입니다.
                    - 필수 포켓 실제 사용액
                    - 자유 포켓 실제 사용액
                    - 미래자산 실제 달성액

                    비상금 및 아직 사용하지 않은 할당 금액은
                    최소 총 예산 계산에 포함하지 않습니다.
                    """
    )
    @PatchMapping("/current/total-budget")
    public ResponseEntity<ApiResponse<TotalBudgetUpdateResponse>> updateTotalBudget(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TotalBudgetUpdateRequest request
    ) {
        return ApiResponse.of(
                BudgetAdjustmentSuccessCode.TOTAL_BUDGET_UPDATED,
                budgetAdjustmentService.updateTotalBudget(
                        userId,
                        request
                )
=======
                BudgetAdjustmentSuccessCode.CURRENT_BUDGET_FOUND,
                budgetAdjustmentService.findCurrent(userId)
>>>>>>> main
        );
    }

    @Operation(
            summary = "현재 예산 재조정",
            description = """
<<<<<<< HEAD
                    진행 중인 이번 달 포켓 예산을 다시 조정합니다.

                    저장 조건:
                    - 네 포켓이 모두 포함되어야 합니다.
                    - 포켓 예산 합계가 총 예산과 같아야 합니다.
                    - 필수/자유 포켓은 실제 사용액보다 낮출 수 없습니다.
                    - 미래자산 포켓은 실제 달성액보다 낮출 수 없습니다.
                    - 비상금 포켓은 0원까지 조정 가능합니다.

                    변경 결과는 예산 변경 이력에도 저장됩니다.
                    """
    )
    @PatchMapping("/current")
    public ResponseEntity<ApiResponse<BudgetAdjustmentResponse>> adjustCurrent(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody BudgetAdjustmentRequest request
=======
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
>>>>>>> main
    ) {

        return ApiResponse.of(
                BudgetAdjustmentSuccessCode.BUDGET_ADJUSTED,
<<<<<<< HEAD
                budgetAdjustmentService.adjustCurrent(
=======
                budgetAdjustmentService.adjust(
>>>>>>> main
                        userId,
                        request
                )
        );
    }
}