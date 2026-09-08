package com.dday.domain.housing.entity;

import com.dday.domain.housing.dto.response.HousingCostResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 월 예상 주거비는 저장하지 않고 계산한다. 그 계산이 화면 값과 맞는지, 그리고
 * "해당 없음"({@code null})과 0원이 뒤섞이지 않는지를 본다.
 */
class HousingCostTest {

    private HousingCost housingCost(Long deposit, Long rent, Long fee) {
        return HousingCost.builder()
                .deposit(deposit)
                .monthlyRent(rent)
                .maintenanceFee(fee)
                .build();
    }

    @Test
    void 월_예상_주거비는_월세와_관리비의_합이다() {
        // 온보딩 화면 값: 월세 450,000 + 관리비 70,000 = 520,000
        assertThat(housingCost(10_000_000L, 450_000L, 70_000L).estimatedMonthly())
                .isEqualTo(520_000L);
    }

    @Test
    void 관리비가_없으면_월세만_센다() {
        assertThat(housingCost(10_000_000L, 450_000L, null).estimatedMonthly())
                .isEqualTo(450_000L);
    }

    @Test
    void 월세가_없으면_관리비만_센다() {
        // 전세라 월세가 없고 관리비만 내는 경우
        assertThat(housingCost(50_000_000L, null, 70_000L).estimatedMonthly())
                .isEqualTo(70_000L);
    }

    @Test
    void 월세도_관리비도_없으면_0이_아니라_null이다() {
        // 가족과 거주 — "주거비 0원"이 아니라 "해당 없음"이다. 화면이 다르게 그려야 한다.
        assertThat(housingCost(null, null, null).estimatedMonthly()).isNull();
    }

    @Test
    void 보증금만_있어도_월_주거비는_null이다() {
        assertThat(housingCost(50_000_000L, null, null).estimatedMonthly()).isNull();
    }

    @Test
    void update는_null을_건너뛰지_않고_지운다() {
        // 이사해서 월세가 사라진 경우. 건너뛰면 옛 월세가 남는다.
        HousingCost cost = housingCost(10_000_000L, 450_000L, 70_000L);

        cost.update(50_000_000L, null, 70_000L);

        assertThat(cost.getMonthlyRent()).isNull();
        assertThat(cost.getDeposit()).isEqualTo(50_000_000L);
        assertThat(cost.estimatedMonthly()).isEqualTo(70_000L);
    }

    @Test
    void 응답에_계산된_월_예상_주거비가_실린다() {
        var response = HousingCostResponse.from(housingCost(10_000_000L, 450_000L, 70_000L));

        assertThat(response.getDeposit()).isEqualTo(10_000_000L);
        assertThat(response.getMonthlyRent()).isEqualTo(450_000L);
        assertThat(response.getMaintenanceFee()).isEqualTo(70_000L);
        assertThat(response.getEstimatedMonthly()).isEqualTo(520_000L);
    }

    @Test
    void 주거비를_입력하지_않은_회원은_빈_응답이다() {
        var response = HousingCostResponse.empty();

        // 404가 아니라 전 필드 null. 온보딩 전은 정상 상태다.
        assertThat(response.getDeposit()).isNull();
        assertThat(response.getEstimatedMonthly()).isNull();
    }
}
