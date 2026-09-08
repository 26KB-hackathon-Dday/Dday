package com.dday.domain.pocket.dto.response;

import com.dday.domain.pocket.entity.Category;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.entity.PocketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 분류 변경 전후의 포켓·카테고리 값을 동일한 구조로 표현한다. */
@Getter
@Builder
@AllArgsConstructor
public class ClassificationValueResponse {
    private Long pocketId;
    private PocketType pocketType;
    private String pocketName;
    private Long categoryId;
    private String categoryName;

    public static ClassificationValueResponse of(Pocket pocket, Category category) {
        return ClassificationValueResponse.builder()
                .pocketId(pocket == null ? null : pocket.getPocketId())
                .pocketType(pocket == null ? null : pocket.getPocketType())
                .pocketName(pocket == null ? null : pocket.getPocketName())
                .categoryId(category == null ? null : category.getCategoryId())
                .categoryName(category == null ? null : category.getCategoryName())
                .build();
    }
}
