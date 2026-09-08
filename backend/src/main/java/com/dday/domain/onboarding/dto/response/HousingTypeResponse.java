package com.dday.domain.onboarding.dto.response;

import com.dday.domain.user.entity.HousingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HousingTypeResponse {

    private final HousingType housingType;

    /**
     * true면 프론트가 주거비 입력 화면을 건너뛴다.
     *
     * <p>현재 enum에는 시설 보호 유형이 없어 항상 false다. 판단은
     * {@code OnboardingService#skipsHousingCost}에 모아뒀다 — 시설 유형이 추가되면 거기만 고친다.
     */
    private final boolean skipHousingCost;

    public static HousingTypeResponse of(HousingType housingType, boolean skipHousingCost) {
        return HousingTypeResponse.builder()
                .housingType(housingType)
                .skipHousingCost(skipHousingCost)
                .build();
    }
}
