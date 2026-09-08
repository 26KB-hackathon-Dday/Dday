package com.dday.domain.credit.service;

import com.dday.domain.credit.dto.response.ExpectedRateResponse;
import com.dday.domain.credit.dto.response.LenderRateResponse;
import com.dday.domain.credit.dto.response.LenderType;
import com.dday.domain.credit.repository.CreditScoreRepository;
import com.dday.domain.credit.service.ExpectedRateCalculator.ExpectedRate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * 최신 신용점수를 업권별 예상 금리와 "점수를 올리면 얼마나 아끼는지"로 바꾼다.
 *
 * <p>금리 표 자체는 {@link ExpectedRateCalculator}가 갖고, 여기서는 <b>돈으로 환산</b>한다 —
 * 어떤 원금을 기준으로 보여줄지는 화면 정책이라 계산기가 아니라 서비스의 몫이다.
 *
 * <p>인터페이스를 두지 않는다 (AGENTS.md §7).
 */
@Service
@RequiredArgsConstructor
public class ExpectedRateService {

    /** 절약액을 재는 기준 원금. 화면 문구 "1,000만원 기준"과 같은 값이다. */
    private static final long SAMPLE_PRINCIPAL = 10_000_000L;

    /** "점수를 이만큼 올리면"의 목표 상승폭. 화면 문구 "신용점수 25점 더 올리면"과 같은 값이다. */
    private static final int TARGET_SCORE_GAIN = 25;

    private static final int MAX_SCORE = 1000;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CreditScoreRepository creditScoreRepository;

    @Transactional(readOnly = true)
    public ExpectedRateResponse findExpected(Long userId) {
        return creditScoreRepository
                .findFirstByUserUserIdOrderByUpdatedAtDescCreditScoreIdDesc(userId)
                .map(creditScore -> build(creditScore.getScore()))
                .orElseGet(ExpectedRateResponse::empty);
    }

    private ExpectedRateResponse build(int score) {
        Integer targetScore = targetScore(score);

        List<LenderRateResponse> lenders = Arrays.stream(LenderType.values())
                .map(lender -> toLenderRate(lender, score, targetScore))
                .toList();

        return ExpectedRateResponse.builder()
                .score(score)
                .principal(SAMPLE_PRINCIPAL)
                .targetScore(targetScore)
                .scoreGap(targetScore == null ? null : targetScore - score)
                .lenders(lenders)
                .build();
    }

    /**
     * 목표 점수 = 현재 + 25점, 만점을 넘지 않는다.
     *
     * <p>이미 만점이면 {@code null} — 더 올릴 여지가 없다는 뜻이고, 프론트는 개선 섹션을 감춘다.
     */
    private Integer targetScore(int score) {
        if (score >= MAX_SCORE) {
            return null;
        }
        return Math.min(score + TARGET_SCORE_GAIN, MAX_SCORE);
    }

    private LenderRateResponse toLenderRate(LenderType lender, int score, Integer targetScore) {
        ExpectedRate current = ExpectedRateCalculator.expectedRateOf(lender, score).orElse(null);
        ExpectedRate target = targetScore == null
                ? null
                : ExpectedRateCalculator.expectedRateOf(lender, targetScore).orElse(null);

        Long currentInterest = annualInterest(current);
        Long targetInterest = annualInterest(target);

        return LenderRateResponse.builder()
                .lenderType(lender)
                .institutionCount(current == null ? null : current.sampleSize())
                .currentRate(current == null ? null : current.rate())
                .currentAnnualInterest(currentInterest)
                .targetRate(target == null ? null : target.rate())
                .targetAnnualInterest(targetInterest)
                .annualSaving(currentInterest == null || targetInterest == null
                        ? null
                        : currentInterest - targetInterest)
                .build();
    }

    /**
     * 연 단리 이자 = 원금 × 금리 ÷ 100, 원 단위로 반올림.
     *
     * <p>거치·상환 방식을 따지지 않는다. 이 화면은 실제 상환액이 아니라 <b>업권 간 차이</b>를
     * 보여주는 자리라, 방식을 섞으면 비교가 흐려진다.
     */
    private Long annualInterest(ExpectedRate expectedRate) {
        if (expectedRate == null) {
            return null;
        }
        return BigDecimal.valueOf(SAMPLE_PRINCIPAL)
                .multiply(expectedRate.rate())
                .divide(HUNDRED, 0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
