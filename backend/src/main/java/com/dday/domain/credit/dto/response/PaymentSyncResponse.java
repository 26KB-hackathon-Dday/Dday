package com.dday.domain.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 데모 납부 이력 동기화 결과. */
@Getter
@Builder
@AllArgsConstructor
public class PaymentSyncResponse {

    /** 이번에 새로 만든 건수. 이미 있던 달은 세지 않으므로 두 번째 호출은 0이 된다. */
    private final int createdCount;

    /** 채우려고 시도한 개월 수. */
    private final int monthsCovered;
}
