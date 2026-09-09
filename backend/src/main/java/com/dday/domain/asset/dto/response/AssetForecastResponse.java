package com.dday.domain.asset.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetForecastResponse {

    /**
     * 현재 보유 자산.
     *
     * 선택된 활성 마이데이터 계좌의 잔액 합계를 우선 사용하고,
     * 선택 계좌가 없으면 온보딩에서 입력한 initialAsset을 사용한다.
     */
    private Long currentAsset;

    /**
     * 지원 종료까지 남은 개월 수.
     *
     * 지원 종료일 = 보호종료일 + 5년
     */
    private Long remainingMonths;
}