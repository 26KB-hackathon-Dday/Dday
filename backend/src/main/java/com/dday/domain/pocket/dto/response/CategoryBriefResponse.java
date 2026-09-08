package com.dday.domain.pocket.dto.response;

import com.dday.domain.pocket.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 거래 응답에 포함되는 최소 카테고리 정보다. 분류되지 않은 거래에서는 객체 자체가 {@code null}이다. */
@Getter
@Builder
@AllArgsConstructor
public class CategoryBriefResponse {

    private Long categoryId;
    private String categoryName;

    public static CategoryBriefResponse from(Category category) {
        // 미분류 거래도 같은 변환 메서드를 사용할 수 있도록 null을 그대로 전달한다.
        return category == null ? null : CategoryBriefResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .build();
    }
}
