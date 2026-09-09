package com.dday.domain.asset.dto;

import com.dday.global.common.code.SuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AssetForecastSuccessCode
        implements SuccessCode {

    ASSET_FORECAST_FOUND(
            HttpStatus.OK,
            "예상 자산 정보를 조회했습니다."
    );

    private final HttpStatus status;

    private final String message;
}