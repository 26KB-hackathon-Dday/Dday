package com.dday.domain.pocket.dto.response;

import com.dday.domain.pocket.entity.Category;
import com.dday.domain.pocket.entity.PocketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/** 카테고리와 바로 아래 자식 카테고리를 재귀적으로 표현하는 응답 노드다. */
@Getter
@Builder
@AllArgsConstructor
public class CategoryResponse {

    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private PocketType defaultPocketType;
    /** 자식이 없어도 JSON에서 빈 배열로 응답하기 위해 기본값을 미리 생성한다. */
    @Builder.Default
    private List<CategoryResponse> children = new ArrayList<>();

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .defaultPocketType(category.getDefaultPocketType())
                .build();
    }
}
