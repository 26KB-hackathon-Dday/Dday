package com.dday.domain.unexpectedincome.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UnexpectedIncomeAddRequest {

    private Long addAmount;

    private List<UnexpectedIncomeAllocationRequest> allocations;
}