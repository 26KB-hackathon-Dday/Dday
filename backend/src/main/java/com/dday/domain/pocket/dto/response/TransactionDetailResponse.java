package com.dday.domain.pocket.dto.response;

import com.dday.domain.mydata.entity.ClassificationSource;
import com.dday.domain.mydata.entity.ClassificationStatus;
import com.dday.domain.mydata.entity.FinancialTransaction;
import com.dday.domain.mydata.entity.TransactionSourceType;
import com.dday.domain.mydata.entity.TransactionStatus;
import com.dday.domain.mydata.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 거래 상세 화면에서 사용하는 외부 출처, 가맹점, 포켓 및 분류 정보를 담는다. */
@Getter
@Builder
@AllArgsConstructor
public class TransactionDetailResponse {

    private Long transactionId;
    private TransactionSourceType sourceType;
    private String sourceTransactionId;
    private LocalDateTime transactionAt;
    private Long amount;
    private String merchantName;
    private String merchantRegno;
    private String memo;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
    private PocketResponse pocket;
    private CategoryBriefResponse category;
    private ClassificationStatus classificationStatus;
    private ClassificationSource classificationSource;

    public static TransactionDetailResponse from(FinancialTransaction transaction) {
        // 자동 분류 전 거래는 포켓과 카테고리가 없을 수 있으므로 null 안전하게 변환한다.
        return TransactionDetailResponse.builder()
                .transactionId(transaction.getFinancialTransactionId())
                .sourceType(transaction.getSourceType())
                .sourceTransactionId(transaction.getSourceTransactionId())
                .transactionAt(transaction.getTransactionAt())
                .amount(transaction.getAmount())
                .merchantName(transaction.getMerchantName())
                .merchantRegno(transaction.getMerchantRegno())
                .memo(transaction.getTransMemo())
                .transactionType(transaction.getTransactionType())
                .transactionStatus(transaction.getTransactionStatus())
                .pocket(transaction.getPocket() == null ? null : PocketResponse.from(transaction.getPocket()))
                .category(CategoryBriefResponse.from(transaction.getCategory()))
                .classificationStatus(transaction.getClassificationStatus())
                .classificationSource(transaction.getClassificationSource())
                .build();
    }
}
