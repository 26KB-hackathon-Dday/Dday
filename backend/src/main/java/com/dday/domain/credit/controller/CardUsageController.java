package com.dday.domain.credit.controller;

import com.dday.domain.credit.dto.CreditSuccessCode;
import com.dday.domain.credit.dto.response.CardUsageResponse;
import com.dday.domain.credit.service.CardUsageService;
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

@Tag(name = "카드 이용률")
@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CardUsageController {

    private final CardUsageService cardUsageService;

    @Operation(summary = "카드 한도 대비 이용률 조회", description = """
            연동한 **신용카드**의 이번 달 이용률과 최근 6개월 추이를 돌려준다.

            **체크·선불카드는 빠진다.** 한도라는 개념이 없어 이용률을 낼 수 없다 —
            0원 한도가 아니라 계산 대상이 아니다. 사용액만 섞으면 이용률이 부풀려진다.

            사용액은 **정상 소비**만 센다(취소·환불 제외). 다른 소비 집계 화면과 같은 기준이다.

            `trend`는 오래된 달부터이고, **거래가 없는 달도 0으로 채운다** — 빼면 그래프의
            가로축이 들쭉날쭉해진다.

            **연동한 신용카드가 없어도 404가 아니다.** `cards: []`, `currentUtilization: null`인
            200을 준다. 이용률이 0%인 것과 구별해야 한다.
            """)
    @GetMapping("/card-usage")
    public ResponseEntity<ApiResponse<CardUsageResponse>> findUsage(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(CreditSuccessCode.CARD_USAGE_FOUND,
                cardUsageService.findUsage(userId));
    }
}
