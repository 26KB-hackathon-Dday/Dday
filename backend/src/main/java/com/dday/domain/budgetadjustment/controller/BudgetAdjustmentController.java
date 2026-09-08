package com.dday.domain.budgetadjustment.controller;

import com.dday.domain.budgetadjustment.dto.BudgetAdjustmentSuccessCode;
import com.dday.domain.budgetadjustment.dto.request.BudgetAdjustmentRequest;
import com.dday.domain.budgetadjustment.dto.response.BudgetAdjustmentResponse;
import com.dday.domain.budgetadjustment.service.BudgetAdjustmentService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "예산 재조정")
@RestController
@RequestMapping("/api/budget-adjustments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BudgetAdjustmentController {

    private final BudgetAdjustmentService budgetAdjustmentService;

    @Operation(
            summary = "이번 달 예산 재조정 정보 조회",
            description = """
                    이미 확정되어 사용 중인 이번 달 예산을 조회합니다.

                    각 포켓별로 다음 정보를 제공합니다.
                    - 현재 예산
                    - 현재 사용액
                    - 재조정 가능한 최소 금액
                    - 남은 예산

                    이미 사용한 금액 아래로는 예산을 낮출 수 없습니다.
                    """
    )
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<BudgetAdjustmentResponse>>
    findCurrent(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.of(
                BudgetAdjustmentSuccessCode
                        .BUDGET_ADJUSTMENT_FOUND,
                budgetAdjustmentService
                        .findCurrent(
                                userId
                        )
        );
    }

    @Operation(
            summary = "이번 달 예산 재조정",
            description = """
                    진행 중인 이번 달 예산을 다시 조정합니다.

                    저장 조건:
                    - 네 포켓이 모두 포함되어야 합니다.
                    - 포켓 예산 합계가 총 예산과 같아야 합니다.
                    - 각 포켓의 새 예산은 이미 사용한 금액 이상이어야 합니다.

                    변경 결과는 예산 변경 이력에도 함께 저장됩니다.
                    """
    )
    @PatchMapping("/current")
    public ResponseEntity<ApiResponse<BudgetAdjustmentResponse>>
    adjustCurrent(
            @AuthenticationPrincipal Long userId,
            @Valid
            @RequestBody
            BudgetAdjustmentRequest request
    ) {
        return ApiResponse.of(
                BudgetAdjustmentSuccessCode
                        .BUDGET_ADJUSTED,
                budgetAdjustmentService
                        .adjustCurrent(
                                userId,
                                request
                        )
        );
    }
}