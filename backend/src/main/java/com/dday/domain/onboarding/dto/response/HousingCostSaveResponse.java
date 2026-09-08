package com.dday.domain.onboarding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 주거비를 저장한 뒤 월 예상 주거비만 돌려준다.
 *
 * <p><b>{@code estimatedMonthly}는 DB에 없는 계산값이다</b> — 월세 + 관리비.
 * 컬럼으로 두면 월세만 고쳤을 때 조용히 어긋난다.
 */
@Getter
@Builder
@AllArgsConstructor
public class HousingCostSaveResponse {

    /** 월세 + 관리비. 둘 다 없으면 null(= 해당 없음)이고 0이 아니다. */
    private final Long estimatedMonthly;

    public static HousingCostSaveResponse of(Long estimatedMonthly) {
        return HousingCostSaveResponse.builder().estimatedMonthly(estimatedMonthly).build();
    }
}
