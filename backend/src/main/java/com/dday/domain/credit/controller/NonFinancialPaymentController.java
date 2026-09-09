package com.dday.domain.credit.controller;

import com.dday.domain.credit.dto.CreditSuccessCode;
import com.dday.domain.credit.dto.response.PaymentHistoryResponse;
import com.dday.domain.credit.dto.response.PaymentSyncResponse;
import com.dday.domain.credit.service.NonFinancialPaymentService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "납부 이력")
@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NonFinancialPaymentController {

    private final NonFinancialPaymentService paymentService;

    @Operation(summary = "비금융 납부 이력 조회", description = """
            통신요금·건강보험료·국민연금을 제때 냈는지를 종류별로 묶어 돌려준다.
            신용평가사가 가점 자료로 받는 항목이라, **카드로 결제한 통신비(거래 내역)와는
            다른 데이터다** — 저쪽은 "얼마 썼나"이고 이쪽은 "기한 안에 냈나"다.

            `onTimeStreak`은 최근부터 연속으로 제때 낸 개월 수다. 연체·미납이 한 번 나오면
            거기서 끊긴다 — "몇 번 냈나"보다 "지금 얼마나 이어지고 있나"가 가점에 의미가 있다.

            `label`·`statusLabel`은 화면에 그대로 쓰는 문구다.

            **이력이 없어도 404가 아니다.** 전부 0인 빈 응답을 준다.
            """)
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<PaymentHistoryResponse>> findHistory(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(CreditSuccessCode.PAYMENTS_FOUND,
                paymentService.findHistory(userId));
    }

    @Operation(summary = "비금융 납부 이력 동기화", description = """
            로그인 회원의 최근 12개월 납부 이력을 채운다.

            **아직 외부 기관이 붙지 않아 서버가 데모 이력을 만들어 넣는다.** 통신사·
            건강보험공단·국민연금공단이 연동되면 이 API의 안쪽만 바뀐다.

            이미 있는 달은 건드리지 않으므로 여러 번 불러도 안전하다 — 두 번째 호출부터
            `createdCount`가 0이 된다.
            """)
    @PostMapping("/payments/sync")
    public ResponseEntity<ApiResponse<PaymentSyncResponse>> sync(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(CreditSuccessCode.PAYMENTS_SYNCED, paymentService.sync(userId));
    }
}
