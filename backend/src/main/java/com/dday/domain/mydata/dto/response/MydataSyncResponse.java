package com.dday.domain.mydata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 한 번의 동기화에서 확인한 원천 데이터와 실제 신규 저장 결과를 요약한다. */
@Getter
@Builder
@AllArgsConstructor
public class MydataSyncResponse {

    private int accountCount;
    private int cardCount;
    private int accountTransactionCount;
    private int cardTransactionCount;
    private int insertedTransactionCount;
    private int skippedTransactionCount;
    private int linkedCancellationCount;
    private LocalDateTime syncedAt;
}
