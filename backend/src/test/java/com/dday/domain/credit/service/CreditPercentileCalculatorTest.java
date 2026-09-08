package com.dday.domain.credit.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditPercentileCalculatorTest {

    @Test
    void 만점이면_상위_0_6퍼센트다() {
        // 950점 이상 1477만명 중 1000점 한 칸의 몫만 앞선다.
        assertThat(CreditPercentileCalculator.percentileOf(1000))
                .isEqualByComparingTo("0.6");
    }

    @Test
    void 최하점이면_상위_100퍼센트다() {
        // 아래에 아무도 없다. 본인을 포함하므로 100을 넘지도 않는다.
        assertThat(CreditPercentileCalculator.percentileOf(1))
                .isEqualByComparingTo("100.0");
    }

    @Test
    void 점수가_높을수록_상위_퍼센트가_작아진다() {
        List<Integer> descendingScores = List.of(1000, 950, 900, 850, 800, 750, 700, 600, 300, 1);

        List<BigDecimal> percentiles = descendingScores.stream()
                .map(CreditPercentileCalculator::percentileOf)
                .toList();

        // 중복이 없어야 "구간이 같으면 같은 값"으로 퉁치는 구현이 통과하지 못한다.
        assertThat(percentiles).doesNotHaveDuplicates().isSorted();
    }

    @Test
    void 구간_경계에서_상위_퍼센트가_역전되지_않는다() {
        // 950과 949는 다른 구간이다. 949가 더 낮은 점수이므로 상위 퍼센트는 더 커야 한다.
        assertThat(CreditPercentileCalculator.percentileOf(950))
                .isLessThan(CreditPercentileCalculator.percentileOf(949));
        assertThat(CreditPercentileCalculator.percentileOf(850))
                .isLessThan(CreditPercentileCalculator.percentileOf(849));
    }

    @Test
    void 같은_구간_안에서도_점수마다_달라진다() {
        // 800과 849는 원본 데이터에서 같은 칸(800점 이상)이다. 보간하지 않으면 같은 값이 나온다.
        assertThat(CreditPercentileCalculator.percentileOf(800))
                .isEqualByComparingTo("58.3");
        assertThat(CreditPercentileCalculator.percentileOf(849))
                .isEqualByComparingTo("52.9");
    }

    @Test
    void 구간의_최저점은_그_구간까지의_누적_비율과_같다() {
        // 보간의 기준점. 구간 최저점에서는 그 구간 전체가 자기 위(자신 포함)에 있다.
        assertThat(CreditPercentileCalculator.percentileOf(950))
                .isEqualByComparingTo("29.2");
        assertThat(CreditPercentileCalculator.percentileOf(900))
                .isEqualByComparingTo("45.1");
    }

    @Test
    void 상위_퍼센트는_소수점_한_자리로_반올림한다() {
        assertThat(CreditPercentileCalculator.percentileOf(812))
                .isEqualTo(new BigDecimal("57.0"));
    }

    @Test
    void 점수_범위를_벗어나면_예외를_던진다() {
        assertThatThrownBy(() -> CreditPercentileCalculator.percentileOf(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CreditPercentileCalculator.percentileOf(1001))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
