package com.dday.domain.budgetadjustment.dto.response;

import com.dday.domain.pocket.entity.PocketType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PocketAdjustmentResponse {

    private Long pocketId;

    private PocketType pocketType;

    private String pocketName;

    /**
     * 현재 이 포켓에 배정된 예산.
     */
    private Long targetAmount;

    /**
     * 이번 달 이미 사용한 금액.
     */
    private Long spentAmount;

    /**
     * 재조정 시 내려갈 수 있는 최소 금액.
     *
     * 현재 정책상 spentAmount와 동일하다.
     */
    private Long minimumAmount;

    /**
     * 현재 남은 예산.
     */
    private Long remainingAmount;
}