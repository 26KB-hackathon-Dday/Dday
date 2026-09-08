package com.dday.domain.mydata.client.dto;

import com.dday.domain.mydata.entity.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 카드 거래 원본이다. 카드 승인 거래는 서비스에서 소비 거래로 저장한다. */
@Getter
@Builder
@AllArgsConstructor
public class MydataCardTransactionData {
    private String transactionId;
    private LocalDateTime transactionAt;
    private Long amount;
    private String merchantName;
    private String merchantRegno;
    private TransactionStatus status;
    private String originalTransactionId;
}
