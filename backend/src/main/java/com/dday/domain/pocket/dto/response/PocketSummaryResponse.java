package com.dday.domain.pocket.dto.response;

import com.dday.domain.pocket.entity.PocketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** 한 포켓의 월 목표와 소비 결과를 표현한다. 포켓 성격상 적용되지 않는 파생 값은 {@code null}이다. */
@Getter
@Builder
@AllArgsConstructor
public class PocketSummaryResponse {

    private Long pocketId;
    private PocketType pocketType;
    private String pocketName;
    /** 월별 포켓 예산으로 배정된 목표 금액(원). */
    private Long targetAmount;
    /** 정상 상태의 소비 거래 합계. 필수·자유 포켓에만 제공한다. */
    private Long usedAmount;
    /** 소비 포켓은 {@code max(목표액-사용액, 0)}, 비상금은 목표액 전체다. */
    private Long remainingAmount;
    /** {@code 사용액 / 목표액 * 100}. 소수 둘째 자리까지 반올림한다. */
    private BigDecimal usageRate;
    /** 목표액을 초과해 소비한 금액. 초과하지 않았으면 0이다. */
    private Long overAmount;
}
