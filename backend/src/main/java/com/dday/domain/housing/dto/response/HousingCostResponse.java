package com.dday.domain.housing.dto.response;

import com.dday.domain.housing.entity.HousingCost;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 주거비 응답.
 *
 * <p>{@code estimatedMonthly}(월 예상 주거비)는 <b>DB에 없는 계산값</b>이다 —
 * {@code monthlyRent + maintenanceFee}. 서버가 계산해 내려주는 이유는 프론트가 각 화면에서
 * 따로 더하면 표기가 갈리기 때문이다 (PocketResponse가 소진율을 서버에서 계산하는 것과 같은 이유).
 *
 * <p>주거비를 아직 입력하지 않은 회원은 {@link #empty()}로 전 필드가 {@code null}인 응답을 준다.
 * 404를 주지 않는 건, 온보딩 전이 정상 상태라 프론트가 에러 분기를 짤 필요가 없어야 해서다.
 */
@Getter
@Builder
@AllArgsConstructor
public class HousingCostResponse {

    private final Long deposit;
    private final Long monthlyRent;
    private final Long maintenanceFee;

    /** 월세 + 관리비. 저장된 값이 아니라 매번 계산한 값이다. */
    private final Long estimatedMonthly;

    public static HousingCostResponse from(HousingCost housingCost) {
        return HousingCostResponse.builder()
                .deposit(housingCost.getDeposit())
                .monthlyRent(housingCost.getMonthlyRent())
                .maintenanceFee(housingCost.getMaintenanceFee())
                .estimatedMonthly(housingCost.estimatedMonthly())
                .build();
    }

    /** 아직 주거비를 입력하지 않은 회원. */
    public static HousingCostResponse empty() {
        return HousingCostResponse.builder().build();
    }
}
