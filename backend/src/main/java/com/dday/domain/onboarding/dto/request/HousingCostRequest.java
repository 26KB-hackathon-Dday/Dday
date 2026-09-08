package com.dday.domain.onboarding.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전부 선택 입력이다 — 주거형태에 따라 해당 없는 항목이 생긴다
 * (가족과 거주면 월세가 없고, 전세면 월세 대신 보증금만 있다).
 *
 * <p>보내지 않은 값은 {@code null}로 저장된다. 0과 "해당 없음"은 다르게 다뤄지므로
 * 프론트가 빈 칸을 0으로 바꿔 보내면 안 된다.
 */
@Getter
@NoArgsConstructor
public class HousingCostRequest {

    @PositiveOrZero(message = "보증금은 0원 이상이어야 합니다.")
    private Long deposit;

    @PositiveOrZero(message = "월세는 0원 이상이어야 합니다.")
    private Long monthlyRent;

    @PositiveOrZero(message = "관리비는 0원 이상이어야 합니다.")
    private Long maintenanceFee;
}
