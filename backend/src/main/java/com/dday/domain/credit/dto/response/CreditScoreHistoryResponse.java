package com.dday.domain.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 최근 신용점수 이력 응답.
 *
 * <p>{@link #latestScore}·{@link #latestPercentile}·{@link #diffFromPrevious}는
 * {@code items[0]}과 같은 값이다. 중복이지만, 화면 상단의 "현재 점수"와 아래 그래프는 서로 다른
 * 컴포넌트라 프론트가 빈 배열을 확인하고 첫 원소를 꺼내는 분기를 매번 짜지 않게 서버가 꺼내 둔다.
 *
 * <p>기록이 없는 회원은 404가 아니라 {@link #empty()}를 받는다. 온보딩 직후 이력이 없는 것은
 * 정상 상태라 프론트가 에러 분기를 짤 필요가 없어야 한다 ({@code HousingCostResponse}와 같은 이유).
 */
@Getter
@Builder
@AllArgsConstructor
public class CreditScoreHistoryResponse {

    /** 가장 최근 점수. 기록이 없으면 {@code null}. */
    private final Integer latestScore;

    /** 가장 최근 점수가 상위 몇 %인지. 기록이 없으면 {@code null}. */
    private final BigDecimal latestPercentile;

    /** 가장 최근 점수의 직전 대비 증감. 비교할 기록이 없으면 {@code null}. */
    private final Integer diffFromPrevious;

    /** 최신순. 최대 다섯 건. */
    private final List<CreditScoreItemResponse> items;

    public static CreditScoreHistoryResponse of(List<CreditScoreItemResponse> items) {
        if (items.isEmpty()) {
            return empty();
        }
        CreditScoreItemResponse latest = items.get(0);
        return CreditScoreHistoryResponse.builder()
                .latestScore(latest.getScore())
                .latestPercentile(latest.getPercentile())
                .diffFromPrevious(latest.getDiff())
                .items(items)
                .build();
    }

    /** 아직 신용점수 기록이 없는 회원. */
    public static CreditScoreHistoryResponse empty() {
        return CreditScoreHistoryResponse.builder()
                .items(List.of())
                .build();
    }
}
