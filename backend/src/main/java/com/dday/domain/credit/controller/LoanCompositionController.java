package com.dday.domain.credit.controller;

import com.dday.domain.credit.dto.CreditSuccessCode;
import com.dday.domain.credit.dto.response.LoanCompositionResponse;
import com.dday.domain.credit.service.LoanCompositionService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대출 구성")
@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class LoanCompositionController {

    private final LoanCompositionService loanCompositionService;

    @Operation(summary = "대출 건수·금융권 조회", description = """
            연동 계좌 중 **대출 계좌만** 골라 몇 건을 어느 금융권에서 받았는지 돌려준다.
            신용점수는 얼마를 빌렸는지보다 **어디서 빌렸는지**에 더 민감하다.

            `sectors`는 제1금융권 → 제2금융권 → 대부업 순이고, 해당 대출이 없는 권역은 빠진다.
            `loans`는 잔액이 큰 순이다.

            **`loanCount`·`totalBalance`와 `sectors`의 합이 다를 수 있다.** 권역을 모르는
            기관의 대출은 총계에는 들어가지만 `sectors`에서는 빠진다 — 모르는 코드를
            제1금융권으로 넘겨짚으면 안내가 실제보다 후해지기 때문이다.

            `label`·`sectorLabel`·`institutionName`은 화면에 그대로 쓰는 문구다.
            모르는 기관 코드면 `institutionName`에 코드가 그대로 온다 — 화면이 비는 것보다 낫다.

            **대출이 없어도 404가 아니다.** 전부 0인 빈 응답을 준다.
            """)
    @GetMapping("/loans")
    public ResponseEntity<ApiResponse<LoanCompositionResponse>> findComposition(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(CreditSuccessCode.LOANS_FOUND,
                loanCompositionService.findComposition(userId));
    }
}
