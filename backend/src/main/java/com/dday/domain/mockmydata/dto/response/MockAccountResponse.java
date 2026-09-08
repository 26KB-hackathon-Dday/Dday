package com.dday.domain.mockmydata.dto.response;

import com.dday.domain.mockmydata.entity.MockAccountType;
import com.dday.domain.mockmydata.entity.MockMydataAccount;
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
                .build();
    }
}
