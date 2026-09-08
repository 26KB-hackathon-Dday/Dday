package com.dday.domain.onboarding.dto.request;

import com.dday.domain.user.entity.SettlementReceived;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AssetRequest {

    /** 모아둔 자산 총액(원). {@code users.initial_asset}에 저장된다. */
    @NotNull(message = "모아둔 자산을 입력해주세요.")
    @PositiveOrZero(message = "자산은 0원 이상이어야 합니다.")
    private Long totalSaved;

    /** RECEIVED · NOT_YET · NONE */
    @NotNull(message = "자립정착금 수령 여부를 선택해주세요.")
    private SettlementReceived settlementReceived;
}
