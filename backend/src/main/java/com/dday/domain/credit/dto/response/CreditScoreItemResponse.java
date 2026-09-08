package com.dday.domain.credit.dto.response;

import com.dday.domain.credit.entity.CreditScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 신용점수 이력 한 건.
 *
 * <p>{@link #diff}는 DB에 없는 계산값이다 — 직전 기록과의 차이를 서버가 매번 계산한다.
 * 프론트가 각 화면에서 따로 빼면 부호나 기준이 갈리기 때문이다.
 */
@Getter
@Builder
@AllArgsConstructor
public class CreditScoreItemResponse {

    private final Long creditScoreId;

    private final Integer score;

    /** 신용평가기관. Mock 데이터 단계에서는 비어 있을 수 있다. */
    private final String agency;

    /** 직전 기록 대비 증감. 비교할 직전 기록이 없는 가장 오래된 항목은 {@code null}이다. */
    private final Integer diff;

    private final LocalDateTime updatedAt;

    public static CreditScoreItemResponse of(CreditScore creditScore, Integer diff) {
        return CreditScoreItemResponse.builder()
                .creditScoreId(creditScore.getCreditScoreId())
                .score(creditScore.getScore())
                .agency(creditScore.getAgency())
                .diff(diff)
                .updatedAt(creditScore.getUpdatedAt())
                .build();
    }
}
