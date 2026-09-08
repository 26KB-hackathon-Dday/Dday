package com.dday.domain.mockmydata.dto.response;

import com.dday.domain.mockmydata.entity.MockMydataCardTransaction;
import com.dday.domain.mockmydata.entity.MockTransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MockCardTransactionResponse {

    private String transactionId;
    private LocalDateTime transactionAt;
    private Long amount;
    private String merchantName;
    private String merchantRegno;
    private MockTransactionStatus status;
    private String originalTransactionId;

    public static MockCardTransactionResponse from(MockMydataCardTransaction transaction) {
        return MockCardTransactionResponse.builder()
                .transactionId(transaction.getExternalTransactionId())
                .transactionAt(transaction.getTransactionAt())
                .amount(transaction.getAmount())
                .merchantName(transaction.getMerchantName())
                .merchantRegno(transaction.getMerchantRegno())
                .status(transaction.getTransactionStatus())
                .originalTransactionId(transaction.getOriginalTransactionId())
                .build();
    }
}
