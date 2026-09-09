package com.dday.domain.mockmydata.dto.response;

import com.dday.domain.mockmydata.entity.MockAccountType;
import com.dday.domain.mockmydata.entity.MockMydataAccount;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MockAccountResponse {

    private String accountId;
    private String orgCode;
    private String accountNum;
    private String accountName;
    private String productName;
    private MockAccountType accountType;
    private Long balance;
    private Long availableBalance;

    /** 연 이자율(%). 대출 계좌만 값이 있다. */
    private BigDecimal interestRate;

    public static MockAccountResponse from(MockMydataAccount account) {
        return MockAccountResponse.builder()
                .accountId(account.getExternalAccountId())
                .orgCode(account.getOrgCode())
                .accountNum(account.getAccountNum())
                .accountName(account.getAccountName())
                .productName(account.getProductName())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .availableBalance(account.getAvailableBalance())
                .interestRate(account.getInterestRate())
                .build();
    }
}
