package com.dday.domain.credit.dto.response;

import com.dday.domain.mydata.entity.FinancialSector;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 한 금융권의 대출 건수와 잔액. */
@Getter
@Builder
@AllArgsConstructor
public class LoanSectorResponse {

    private final FinancialSector sector;

    /** 화면에 그대로 쓰는 금융권 문구. */
    private final String label;

    private final int loanCount;

    private final long totalBalance;
}
