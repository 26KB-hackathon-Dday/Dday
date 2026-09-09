package com.dday.domain.pocket.dto.response;

import com.dday.domain.pocket.entity.PocketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 월말 기준 필수·자유 포켓의 예산 소진 결과다. */
@Getter
@Builder
@AllArgsConstructor
public class MonthlyPocketSettlementResponse {

    /** 정산 대상 월. {@code yyyy-MM} 형식으로 반환한다. */
    private String month;
    /** 정산에 포함된 필수·자유 포켓의 목표 금액 합계다. */
    private Long totalTargetAmount;
    /** 정산 대상 포켓에 연결된 정상 소비 거래의 합계다. */
    private Long totalUsedAmount;
    /** 목표액 안에서 사용하고 남은 금액의 포켓별 합계다. */
    private Long totalRemainingAmount;
    /** 목표액을 넘겨 사용한 금액의 포켓별 합계다. */
    private Long totalOverAmount;
    /** 필수 포켓, 자유 포켓 순서로 정렬된 개별 정산 결과다. */
    private List<PocketSettlement> pockets;

    /** 한 포켓의 월말 정산 결과다. 잔액과 초과액은 동시에 양수가 될 수 없다. */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class PocketSettlement {

        private Long pocketId;
        private PocketType pocketType;
        private String pocketName;
        private Long targetAmount;
        private Long usedAmount;
        private Long remainingAmount;
        private Long overAmount;
    }
}
