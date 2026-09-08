package com.dday.domain.mydata.client.dto;

import com.dday.domain.mydata.entity.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 계좌 거래 원본이다. TRANSFER는 저장 단계에서 본인 이체인지 판별한다. */
@Getter
@Builder
@AllArgsConstructor
public class MydataAccountTransactionData {
    private String transactionId;
    private LocalDateTime transactionAt;
    private String transactionType;
    private Long amount;
    private String counterpartyAccountNum;
    private String merchantName;
    private String merchantRegno;
    private String memo;
    private TransactionStatus status;
    private String originalTransactionId;
}
