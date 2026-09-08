package com.dday.domain.credit.service;

import com.dday.domain.credit.dto.response.LenderType;
import com.dday.domain.credit.service.ExpectedRateCalculator.ExpectedRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpectedRateCalculatorTest {

    private BigDecimal rate(LenderType lender, int score) {
        return ExpectedRateCalculator.expectedRateOf(lender, score)
                .map(ExpectedRate::rate)
                .orElse(null);
    }

    @Test
    void 같은_구간_안에서도_점수마다_금리가_다르다() {
        // 704와 729는 은행 공시에서 같은 칸(750~701)이다. 보간하지 않으면 같은 값이 나온다.
        assertThat(rate(LenderType.BANK, 704)).isEqualByComparingTo("6.99");
        assertThat(rate(LenderType.BANK, 729)).isEqualByComparingTo("6.90");
        assertThat(rate(LenderType.CAPITAL, 704)).isEqualByComparingTo("16.07");
        assertThat(rate(LenderType.CARD, 704)).isEqualByComparingTo("15.36");
    }

    @Test
    void 구간_경계에서_금리가_튀지_않는다() {
        // 공시값을 구간 중앙에 놓고 이었기 때문에 750점과 751점 사이에 계단이 없어야 한다.
        BigDecimal atBoundary = rate(LenderType.BANK, 750);
        BigDecimal justAbove = rate(LenderType.BANK, 751);

        assertThat(atBoundary.subtract(justAbove).abs()).isLessThanOrEqualTo(new BigDecimal("0.01"));
    }

    @Test
    void 양_끝_중앙값_바깥은_공시값으로_고정된다() {
        // 외삽하지 않는다. 최고 구간 중앙(975.5) 위로는 근거가 없으므로 공시값을 그대로 쓴다.
        assertThat(rate(LenderType.BANK, 1000)).isEqualByComparingTo("4.93");
        assertThat(rate(LenderType.BANK, 990)).isEqualByComparingTo("4.93");
        // 캐피탈 최저 구간 중앙(550.5) 아래도 마찬가지다.
        assertThat(rate(LenderType.CAPITAL, 510)).isEqualByComparingTo("17.53");
    }

    @Test
    void 전_구간에서_금리가_역전되지_않는다() {
        for (LenderType lender : LenderType.values()) {
            BigDecimal previous = null;
            for (int score = 1; score <= 1000; score++) {
                BigDecimal current = rate(lender, score);
                if (current == null) {
                    continue;
                }
                if (previous != null) {
                    assertThat(current).as("%s %d점", lender, score).isLessThanOrEqualTo(previous);
                }
                previous = current;
            }
        }
    }

    @Test
    void 공시에_없는_구간은_금리가_없다() {
        // 카드사는 500점 이하 공시값이 전부 0.00 — 금리 0%가 아니라 취급이 없다는 뜻이다.
        assertThat(ExpectedRateCalculator.expectedRateOf(LenderType.CARD, 400)).isEmpty();
        assertThat(ExpectedRateCalculator.expectedRateOf(LenderType.CAPITAL, 400)).isEmpty();
        // 은행은 600점 이하까지 전 구간을 덮는다.
        assertThat(ExpectedRateCalculator.expectedRateOf(LenderType.BANK, 400)).isPresent();
    }

    @Test
    void 회사_수는_점수가_속한_구간_기준이다() {
        assertThat(ExpectedRateCalculator.expectedRateOf(LenderType.BANK, 704))
                .get().extracting(ExpectedRate::sampleSize).isEqualTo(15);
        assertThat(ExpectedRateCalculator.expectedRateOf(LenderType.CAPITAL, 704))
                .get().extracting(ExpectedRate::sampleSize).isEqualTo(8);
        // 카드사 900~801 구간은 한 곳이 공시하지 않아 다섯 곳 평균이다.
        assertThat(ExpectedRateCalculator.expectedRateOf(LenderType.CARD, 704))
                .get().extracting(ExpectedRate::sampleSize).isEqualTo(5);
    }

    @Test
    void 점수_범위를_벗어나면_예외를_던진다() {
        assertThatThrownBy(() -> ExpectedRateCalculator.expectedRateOf(LenderType.BANK, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpectedRateCalculator.expectedRateOf(LenderType.BANK, 1001))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
