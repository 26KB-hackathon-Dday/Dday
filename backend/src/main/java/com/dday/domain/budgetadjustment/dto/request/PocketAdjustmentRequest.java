package com.dday.domain.budgetadjustment.dto.request;

import com.dday.domain.pocket.entity.PocketType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PocketAdjustmentRequest {

    private PocketType pocketType;

    private Long amount;
}