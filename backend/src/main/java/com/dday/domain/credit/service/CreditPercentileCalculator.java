package com.dday.domain.credit.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * 신용점수 하나를 받아 "상위 몇 %"인지 계산한다.
 *
 * <p><b>KCB(올크레딧) 2025년말 인원분포 기준이다.</b> NICE는 분포가 달라 이 표를 쓰면 안 된다 —
 * {@code CreditScore.agency}에는 KCB와 NICE가 모두 들어올 수 있으므로, NICE 분포를 확보하면
 * 그때 기관별로 표를 나눈다. 지금 기관을 인자로 받지 않는 건 표가 하나뿐이라서다.
 *
 * <p>출처: <a href="https://www.allcredit.co.kr/screen/sc6822450412">올크레딧 신용점수 인원분포</a>
 *
 * <p>{@code OnboardingCalculator}와 같은 순수 계산기다. 상태도 의존성도 없어 스프링 빈으로 두지 않는다.
 */
public final class CreditPercentileCalculator {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 1000;

    /** 상위 %의 소수 자릿수. */
    private static final int SCALE = 1;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * 점수 구간 하나와 그 구간의 인원수.
     *
     * <p>내부 값 타입이라 {@code record}를 쓴다. AGENTS.md §5의 record 금지는 Request·Response
     * DTO에 대한 규칙이고, 이건 외부로 나가지 않는다 ({@code RuleHit}·{@code Classification}과 같다).
     */
    private record ScoreBucket(int from, int to, long headcount) {

        /** 구간에 속한 점수 개수. 양끝을 모두 포함한다. */
        int width() {
            return to - from + 1;
        }

        boolean covers(int score) {
            return from <= score && score <= to;
        }
    }

    /**
     * KCB 2025년말 신용점수별 인원분포. 높은 점수부터 나열한다.
     *
     * <p>원문의 비율(%) 열이 아니라 <b>인원수</b>를 옮겼다. 비율 열은 반올림돼 합이 99.90%지만
     * 인원수는 총계 50,586,810명과 정확히 맞아떨어진다. 누적 비율은 여기서 직접 계산한다.
     *
     * <p>원문은 "950점 이상", "900점 이상"처럼 적혀 있지만 인원수가 서로 겹치지 않으므로
     * 실제로는 950~1000, 900~949 … 로 끊어진 구간이다.
     *
     * <p>기준 시점이 바뀌면 이 표만 갈아끼운다.
     */
    private static final List<ScoreBucket> KCB_2025 = List.of(
            new ScoreBucket(950, 1000, 14_772_074L),
            new ScoreBucket(900, 949, 8_063_748L),
            new ScoreBucket(850, 899, 3_874_462L),
            new ScoreBucket(800, 849, 2_802_332L),
            new ScoreBucket(750, 799, 5_182_958L),
            new ScoreBucket(700, 749, 8_365_868L),
            new ScoreBucket(600, 699, 4_676_261L),
            new ScoreBucket(300, 599, 863_973L),
            new ScoreBucket(1, 299, 1_985_134L));

    private static final BigDecimal TOTAL_HEADCOUNT = BigDecimal.valueOf(
            KCB_2025.stream().mapToLong(ScoreBucket::headcount).sum());

    private CreditPercentileCalculator() {
    }

    /**
     * 이 점수가 상위 몇 %인지. 소수점 한 자리로 반올림한다.
     *
     * <p>결과는 항상 {@code 0 < x <= 100}이다. <b>본인을 포함해</b> 세기 때문이다 —
     * 빼면 만점이 "상위 0.0%"가 되는데, 그건 "나보다 위에 아무도 없다"는 뜻이지
     * 상위 몇 %가 아니다. 그래서 만점은 0.6%, 최하점은 100%가 된다.
     *
     * @throws IllegalArgumentException 점수가 1~1000을 벗어나면. 사용자 입력이 아니라 우리 DB에서
     *                                  오는 값이라, 벗어났다면 데이터나 코드가 깨진 것이다
     */
    public static BigDecimal percentileOf(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new IllegalArgumentException(
                    "신용점수는 %d~%d 사이여야 한다: %d".formatted(MIN_SCORE, MAX_SCORE, score));
        }

        long above = 0;
        for (ScoreBucket bucket : KCB_2025) {
            if (bucket.covers(score)) {
                return toPercent(BigDecimal.valueOf(above).add(headcountWithin(bucket, score)));
            }
            above += bucket.headcount();
        }
        throw new IllegalStateException("분포 표가 1~1000을 다 덮지 않는다: " + score);
    }

    /**
     * 같은 구간 안에서 이 점수 이상인 인원. 구간 안은 <b>균등 분포로 가정</b>한다.
     *
     * <p>가정이라는 점을 분명히 해둔다 — 원본 데이터에는 구간 안의 분포가 없다.
     * 구간 기준으로만 답하면 849점과 850점이 5.5%p 차이로 튀는 게 더 어색해서 택한 방식이다.
     */
    private static BigDecimal headcountWithin(ScoreBucket bucket, int score) {
        return BigDecimal.valueOf(bucket.headcount())
                .multiply(BigDecimal.valueOf(bucket.to() - score + 1))
                .divide(BigDecimal.valueOf(bucket.width()), MathContext.DECIMAL64);
    }

    private static BigDecimal toPercent(BigDecimal headcountAbove) {
        return headcountAbove.multiply(HUNDRED)
                .divide(TOTAL_HEADCOUNT, SCALE, RoundingMode.HALF_UP);
    }
}
