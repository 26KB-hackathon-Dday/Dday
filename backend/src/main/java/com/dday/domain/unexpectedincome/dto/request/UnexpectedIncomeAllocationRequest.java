package com.dday.domain.unexpectedincome.dto.request;

import com.dday.domain.pocket.entity.PocketType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UnexpectedIncomeAllocationRequest {

    private PocketType pocketType;

    private Long amount;
}