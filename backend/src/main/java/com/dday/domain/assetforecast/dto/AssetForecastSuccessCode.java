package com.dday.domain.assetforecast.dto;

import com.dday.global.common.code.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AssetForecastSuccessCode implements SuccessCode {

    ASSET_FORECAST_FOUND(
            HttpStatus.OK,
            "지원 종료 시 예상 자산 조회에 성공했습니다."
    );

    private final HttpStatus status;
    private final String message;
}