package com.dday.domain.mydata.client.dto;

import com.dday.domain.mydata.entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 외부 MyData 계좌 응답을 서비스가 이해하는 타입으로 정규화한 값 객체다. */
@Getter
@Builder
@AllArgsConstructor
public class MydataAccountData {
    private String externalAccountId;
    private String orgCode;
    private String accountNum;
    private String accountName;
    private String productName;
    private AccountType accountType;
    private Long balance;
    private Long availableBalance;
}
