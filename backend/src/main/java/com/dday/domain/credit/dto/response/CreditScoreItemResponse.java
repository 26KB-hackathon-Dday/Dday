package com.dday.domain.credit.dto.response;

import com.dday.domain.credit.entity.CreditScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 신용점수 이력 한 건.
 *
 * <p>{@link #diff}와 {@link #percentile}은 DB에 없는 계산값이다. 프론트가 각 화면에서 따로
 * 구하면 기준이 갈리므로 서버가 매번 계산해 내려준다.
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

    /** 이 점수가 상위 몇 %인지. KCB 2025년말 인원분포 기준이다. */
    private final BigDecimal percentile;

    private final LocalDateTime updatedAt;

    public static CreditScoreItemResponse of(CreditScore creditScore, Integer diff,
                                             BigDecimal percentile) {
        return CreditScoreItemResponse.builder()
                .creditScoreId(creditScore.getCreditScoreId())
                .score(creditScore.getScore())
                .agency(creditScore.getAgency())
                .diff(diff)
                .percentile(percentile)
                .updatedAt(creditScore.getUpdatedAt())
                .build();
    }
}
