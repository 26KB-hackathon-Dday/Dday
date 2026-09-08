package com.dday.domain.onboarding.dto.request;

import com.dday.domain.user.entity.HousingType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HousingTypeRequest {

    /** enum에 없는 값이 오면 Jackson이 400을 낸다 — 서비스까지 내려오지 않는다. */
    @NotNull(message = "주거형태를 선택해주세요.")
    private HousingType housingType;
}
