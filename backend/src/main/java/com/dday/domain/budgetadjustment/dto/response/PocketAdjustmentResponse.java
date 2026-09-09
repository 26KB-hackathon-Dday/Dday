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

    private Long targetAmount;

    private Long usedAmount;

    private Long remainingAmount;
}