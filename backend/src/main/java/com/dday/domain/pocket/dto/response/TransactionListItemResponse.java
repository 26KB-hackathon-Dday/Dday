package com.dday.domain.pocket.dto.response;

import com.dday.domain.mydata.entity.ClassificationSource;
import com.dday.domain.mydata.entity.ClassificationStatus;
import com.dday.domain.mydata.entity.FinancialTransaction;
import com.dday.domain.mydata.entity.TransactionStatus;
import com.dday.domain.mydata.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 거래 목록 화면에 필요한 표시·분류 정보만 담는 가벼운 응답이다. */
@Getter
@Builder
@AllArgsConstructor
public class TransactionListItemResponse {

    private Long transactionId;
    private LocalDateTime transactionAt;
    private String merchantName;
    private String memo;
    private Long amount;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
    private CategoryBriefResponse category;
    private ClassificationStatus classificationStatus;
    private ClassificationSource classificationSource;

    public static TransactionListItemResponse from(FinancialTransaction transaction) {
        // 목록에서 사용하지 않는 출처 상세와 연관 거래는 의도적으로 응답에서 제외한다.
        return TransactionListItemResponse.builder()
                .transactionId(transaction.getFinancialTransactionId())
                .transactionAt(transaction.getTransactionAt())
                .merchantName(transaction.getMerchantName())
                .memo(transaction.getTransMemo())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .transactionStatus(transaction.getTransactionStatus())
                .category(CategoryBriefResponse.from(transaction.getCategory()))
                .classificationStatus(transaction.getClassificationStatus())
                .classificationSource(transaction.getClassificationSource())
                .build();
    }
}
