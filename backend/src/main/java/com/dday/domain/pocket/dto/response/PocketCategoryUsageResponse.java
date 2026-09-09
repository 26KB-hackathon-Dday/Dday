package com.dday.domain.pocket.dto.response;

import com.dday.domain.pocket.entity.PocketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 한 달 동안 특정 소비 포켓에서 카테고리별로 사용한 실제 금액을 담는다. */
@Getter
@Builder
@AllArgsConstructor
public class PocketCategoryUsageResponse {

    private String month;
    private PocketType pocketType;
    private Long totalUsedAmount;
    private List<CategoryUsageResponse> categories;
    private Long unclassifiedUsedAmount;

    /** 카테고리 기준정보와 해당 월의 정상 소비 합계를 결합한 항목이다. */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CategoryUsageResponse {
        private Long categoryId;
        private String categoryCode;
        private String categoryName;
        private Long usedAmount;
    }
}
