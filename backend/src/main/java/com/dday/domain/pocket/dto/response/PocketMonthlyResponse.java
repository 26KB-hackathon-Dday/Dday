package com.dday.domain.pocket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 요청 월의 전체 예산과 네 포켓의 요약 정보를 묶는 응답이다. */
@Getter
@Builder
@AllArgsConstructor
public class PocketMonthlyResponse {

    /** 조회 기준 월. {@code yyyy-MM} 형식이다. */
    private String month;
    /** 해당 월에 설정한 전체 예산(원). */
    private Long totalBudgetAmount;
    /** 포켓 유형 선언 순서(필수, 자유, 비상금, 미래자산)의 월간 현황. */
    private List<PocketSummaryResponse> pockets;
}
