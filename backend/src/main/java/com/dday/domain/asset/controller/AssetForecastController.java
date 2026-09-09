package com.dday.domain.asset.controller;

import com.dday.domain.asset.dto.AssetForecastSuccessCode;
import com.dday.domain.asset.dto.response.AssetForecastResponse;
import com.dday.domain.asset.service.AssetForecastService;
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

@Tag(name = "예상 자산")
@RestController
@RequestMapping("/api/asset-forecast")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AssetForecastController {

    private final AssetForecastService assetForecastService;

    @Operation(
            summary = "예상 자산 계산 기준 조회",
            description = """
                    포켓 예산 조정 화면의 예상 자산 계산에 필요한 값을 조회합니다.

                    currentAsset:
                    - 사용자가 선택한 활성 마이데이터 계좌 잔액 합계
                    - 선택 계좌가 없으면 온보딩 initialAsset

                    remainingMonths:
                    - 보호종료일 + 5년까지 남은 개월 수
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<AssetForecastResponse>>
    find(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.of(
                AssetForecastSuccessCode
                        .ASSET_FORECAST_FOUND,
                assetForecastService
                        .find(userId)
        );
    }
}