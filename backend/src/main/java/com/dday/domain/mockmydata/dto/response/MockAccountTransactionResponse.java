package com.dday.domain.mockmydata.dto.response;

import com.dday.domain.mockmydata.entity.MockMydataAccountTransaction;
import com.dday.domain.mockmydata.entity.MockTransactionStatus;
import com.dday.domain.mockmydata.entity.MockTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MockAccountTransactionResponse {

    private String transactionId;
    private LocalDateTime transactionAt;
    private MockTransactionType transactionType;
    private Long amount;
    private String counterpartyName;
    private String counterpartyAccountNum;
    private String merchantName;
    private String merchantRegno;
    private String memo;
    private MockTransactionStatus status;
    private String originalTransactionId;

    public static MockAccountTransactionResponse from(MockMydataAccountTransaction transaction) {
        return MockAccountTransactionResponse.builder()
                .transactionId(transaction.getExternalTransactionId())
                .transactionAt(transaction.getTransactionAt())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .counterpartyName(transaction.getCounterpartyName())
                .counterpartyAccountNum(transaction.getCounterpartyAccountNum())
                .merchantName(transaction.getMerchantName())
                .merchantRegno(transaction.getMerchantRegno())
                .memo(transaction.getTransMemo())
                .status(transaction.getTransactionStatus())
                .originalTransactionId(transaction.getOriginalTransactionId())
                .build();
    }
}
