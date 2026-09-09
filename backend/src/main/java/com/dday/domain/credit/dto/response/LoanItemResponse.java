package com.dday.domain.credit.dto.response;

import com.dday.domain.mydata.entity.FinancialSector;
import com.dday.domain.mydata.entity.Institution;
import com.dday.domain.mydata.entity.UserAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** 대출 한 건. */
@Getter
@Builder
@AllArgsConstructor
public class LoanItemResponse {

    private final Long accountId;

    /** 기관 이름. 모르는 코드면 코드가 그대로 온다 — 화면이 비는 것보다 낫다. */
    private final String institutionName;

    /** 금융권. 모르는 기관이면 {@code null}. */
    private final FinancialSector sector;

    /** 화면에 그대로 쓰는 금융권 문구. 권역을 모르면 {@code null}. */
    private final String sectorLabel;

    private final String productName;

    /** 남은 원금(원). */
    private final Long balance;

    /** 연 이자율(%). 없을 수도 있다. */
    private final BigDecimal interestRate;

    public static LoanItemResponse from(UserAccount account) {
        FinancialSector sector = Institution.sectorOf(account.getOrgCode());
        return LoanItemResponse.builder()
                .accountId(account.getAccountId())
                .institutionName(Institution.nameOf(account.getOrgCode()))
                .sector(sector)
                .sectorLabel(sector == null ? null : sector.getLabel())
                .productName(account.getProductName())
                .balance(account.getBalance())
                .interestRate(account.getInterestRate())
                .build();
    }
}
