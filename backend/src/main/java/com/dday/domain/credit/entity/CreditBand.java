package com.dday.domain.credit.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 신용점수 구간별로 "이 점수면 대출·카드가 어느 정도 되는지"를 담은 기준정보.
 *
 * <p>사용자별 데이터가 아니라 전역 표다. 점수 하나를 받아 이 표에서 구간을 찾아
 * 예상 금리·승인율을 화면에 띄운다. 값이 바뀌면 이 표만 갈아끼우면 된다.
 *
 * <p>비율은 {@link BigDecimal}이다. 금리 3.5%를 {@code double}로 두면 표시할 때
 * 3.4999…가 튀어나온다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "credit_band")
public class CreditBand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credit_band_id")
    private Long creditBandId;

    /** 구간 시작 점수 (1~1000, 이상). */
    @Column(name = "score_from", nullable = false, columnDefinition = "SMALLINT")
    private Integer scoreFrom;

    /** 구간 끝 점수 (1~1000, 이하). */
    @Column(name = "score_to", nullable = false, columnDefinition = "SMALLINT")
    private Integer scoreTo;

    /** 상위 몇 %인지. 0~100. */
    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer percentile;

    /** 예상 대출 금리(%). */
    @Column(name = "loan_interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal loanInterestRate;

    /** 예상 대출 승인율(%). */
    @Column(name = "loan_approval_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal loanApprovalRate;

    /** 예상 카드 최대 한도. */
    @Column(name = "card_max_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal cardMaxLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_issue_possibility", nullable = false, length = 20)
    private CardIssuePossibility cardIssuePossibility;

    @Builder
    private CreditBand(Integer scoreFrom, Integer scoreTo, Integer percentile,
                       BigDecimal loanInterestRate, BigDecimal loanApprovalRate,
                       BigDecimal cardMaxLimit, CardIssuePossibility cardIssuePossibility) {
        this.scoreFrom = scoreFrom;
        this.scoreTo = scoreTo;
        this.percentile = percentile;
        this.loanInterestRate = loanInterestRate;
        this.loanApprovalRate = loanApprovalRate;
        this.cardMaxLimit = cardMaxLimit;
        this.cardIssuePossibility = cardIssuePossibility;
    }

    /** 점수가 이 구간에 드는지. 양끝 모두 포함이다. */
    public boolean covers(int score) {
        return this.scoreFrom <= score && score <= this.scoreTo;
    }
}
