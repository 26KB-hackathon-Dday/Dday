package com.dday.domain.assetforecast.controller;

import com.dday.domain.assetforecast.dto.AssetForecastSuccessCode;
import com.dday.domain.assetforecast.dto.response.AssetForecastResponse;
import com.dday.domain.assetforecast.service.AssetForecastService;
import com.dday.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/asset-forecast")
public class AssetForecastController {

    private final AssetForecastService assetForecastService;

    /**
     * 지원 종료 시 예상 총자산 조회.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AssetForecastResponse>> getForecast(
            @AuthenticationPrincipal Long userId
    ) {

        AssetForecastResponse response =
                assetForecastService.getForecast(
                        userId
                );

        return ApiResponse.of(
                AssetForecastSuccessCode.ASSET_FORECAST_FOUND,
                response
        );
    }
}