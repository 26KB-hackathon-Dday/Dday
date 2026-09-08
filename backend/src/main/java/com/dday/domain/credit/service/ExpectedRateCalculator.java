package com.dday.domain.credit.service;

import com.dday.domain.credit.dto.response.LenderType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 신용점수 하나를 받아 업권별 예상 금리를 계산한다.
 *
 * <p><b>KCB 기준으로 공시한 회사만 평균에 넣었다.</b> 같은 표에 NICE 기준 회사가 섞여 있는데,
 * 두 평가사는 점수 척도가 달라 한 점수를 서로 다른 자로 잰 값이 된다. 상위 % 모듈
 * ({@code CreditPercentileCalculator})도 KCB 기준이라 척도를 맞춰야 한 화면에서 같이 읽힌다.
 *
 * <p>출처
 * <ul>
 *   <li>은행: 은행연합회 소비자포털 가계대출 신용점수별 금리현황 (2026-09-08 내려받음, KCB 15곳)</li>
 *   <li>캐피탈·카드사: 여신금융협회 신용점수별 평균금리현황, 신용평가사 신용점수 기준
 *       (2026-08-20 게시, KCB 캐피탈 8곳·카드사 6곳)</li>
 * </ul>
 *
 * <p><b>{@code 0.00}과 {@code "-"}는 평균에서 뺐다.</b> 금리 0%가 아니라 그 구간에 취급 실적이
 * 없거나 미제출이라는 뜻이다. 그대로 더하면 저신용 구간 평균이 실제보다 훨씬 낮게 나온다.
 * 한 곳도 없는 구간은 표에서 빼서 {@code Optional.empty()}가 나오게 했다.
 *
 * <p><b>구간 사이는 선형 보간한다.</b> 공시 구간은 은행 50점, 캐피탈·카드사 100점 단위라
 * 구간 값을 그대로 쓰면 25점을 올려도 금리가 그대로인 점수대가 대부분이다. 공시값을 구간
 * <b>중앙</b>에 놓고 이웃 중앙값끼리 이었다 — 공시값이 "그 구간의 평균"이므로 구간 한가운데를
 * 대표점으로 보는 게 맞고, 그래야 경계에서 계단이 생기지 않는다. <b>보간으로 만든 값은 공시된
 * 수치가 아니다.</b> 양 끝 중앙값 바깥은 근거가 없으므로 외삽하지 않고 공시값으로 고정한다.
 *
 * <p>{@code CreditPercentileCalculator}와 같은 순수 계산기다. 상태도 의존성도 없다.
 */
public final class ExpectedRateCalculator {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 1000;

    /** 금리 소수 자릿수. 공시값과 같은 정밀도로 맞춘다. */
    private static final int RATE_SCALE = 2;

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    /** 예상 금리와, 점수가 속한 구간의 평균에 들어간 회사 수. */
    public record ExpectedRate(BigDecimal rate, int sampleSize) {
    }

    /**
     * 공시 구간 하나.
     *
     * <p>내부 값 타입이라 {@code record}를 쓴다 (AGENTS.md §5의 record 금지는 Request·Response
     * DTO에 대한 규칙이다).
     */
    private record RateBand(int from, int to, String rate, int sampleSize) {

        boolean covers(int score) {
            return from <= score && score <= to;
        }

        /** 보간의 대표점. 공시값은 이 구간의 평균이므로 구간 한가운데에 놓는다. */
        BigDecimal midpoint() {
            return BigDecimal.valueOf((long) from + to).divide(TWO);
        }

        BigDecimal rateValue() {
            return new BigDecimal(rate);
        }
    }

    /** 은행 15곳(KCB)의 구간별 평균 금리. 한국산업은행은 전 구간 미제출이라 애초에 빠진다. */
    private static final List<RateBand> BANK_BANDS = List.of(
            new RateBand(951, 1000, "4.93", 15),
            new RateBand(901, 950, "5.23", 15),
            new RateBand(851, 900, "5.88", 15),
            new RateBand(801, 850, "6.33", 15),
            new RateBand(751, 800, "6.77", 15),
            new RateBand(701, 750, "6.91", 15),
            new RateBand(651, 700, "7.09", 15),
            new RateBand(601, 650, "7.57", 15),
            new RateBand(1, 600, "8.19", 15));

    /** 캐피탈 8곳(KCB)의 구간별 평균 금리. 500점 이하는 공시한 곳이 없다. */
    private static final List<RateBand> CAPITAL_BANDS = List.of(
            new RateBand(901, 1000, "13.45", 8),
            new RateBand(801, 900, "14.53", 8),
            new RateBand(701, 800, "15.61", 8),
            new RateBand(601, 700, "16.59", 8),
            new RateBand(501, 600, "17.53", 7));

    /** 카드사 6곳(KCB)의 구간별 평균 금리. 500점 이하는 공시한 곳이 없다. */
    private static final List<RateBand> CARD_BANDS = List.of(
            new RateBand(901, 1000, "13.38", 6),
            new RateBand(801, 900, "13.98", 5),
            new RateBand(701, 800, "14.97", 5),
            new RateBand(601, 700, "15.81", 5),
            new RateBand(501, 600, "18.27", 4));

    private static final Map<LenderType, List<RateBand>> BANDS = Map.of(
            LenderType.BANK, BANK_BANDS,
            LenderType.CAPITAL, CAPITAL_BANDS,
            LenderType.CARD, CARD_BANDS);

    private ExpectedRateCalculator() {
    }

    /**
     * 이 점수의 업권 예상 금리.
     *
     * @return 그 점수대의 공시가 아예 없으면 {@code empty} — 금리 0%와 반드시 구별해야 한다
     * @throws IllegalArgumentException 점수가 1~1000을 벗어나면
     */
    public static Optional<ExpectedRate> expectedRateOf(LenderType lender, int score) {
        validate(score);
        List<RateBand> bands = BANDS.get(lender);
        return bands.stream()
                .filter(band -> band.covers(score))
                .findFirst()
                .map(band -> new ExpectedRate(interpolate(bands, score), band.sampleSize()));
    }

    /** 이웃한 두 구간의 중앙값 사이를 직선으로 잇는다. 바깥은 외삽하지 않고 끝값으로 고정한다. */
    private static BigDecimal interpolate(List<RateBand> bands, int score) {
        List<RateBand> ascending = bands.stream()
                .sorted(Comparator.comparing(RateBand::midpoint))
                .toList();
        BigDecimal point = BigDecimal.valueOf(score);

        RateBand lowest = ascending.get(0);
        RateBand highest = ascending.get(ascending.size() - 1);
        if (point.compareTo(lowest.midpoint()) <= 0) {
            return scaled(lowest.rateValue());
        }
        if (point.compareTo(highest.midpoint()) >= 0) {
            return scaled(highest.rateValue());
        }

        for (int i = 0; i < ascending.size() - 1; i++) {
            RateBand lower = ascending.get(i);
            RateBand upper = ascending.get(i + 1);
            if (point.compareTo(upper.midpoint()) <= 0) {
                BigDecimal ratio = point.subtract(lower.midpoint())
                        .divide(upper.midpoint().subtract(lower.midpoint()), MathContext.DECIMAL64);
                BigDecimal step = upper.rateValue().subtract(lower.rateValue()).multiply(ratio);
                return scaled(lower.rateValue().add(step));
            }
        }
        throw new IllegalStateException("중앙값 구간을 찾지 못했다: " + score);
    }

    private static BigDecimal scaled(BigDecimal rate) {
        return rate.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private static void validate(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new IllegalArgumentException(
                    "신용점수는 %d~%d 사이여야 한다: %d".formatted(MIN_SCORE, MAX_SCORE, score));
        }
    }
}
