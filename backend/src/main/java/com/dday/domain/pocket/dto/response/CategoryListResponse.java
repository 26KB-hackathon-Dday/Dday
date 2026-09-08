package com.dday.domain.pocket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 최상위 카테고리 목록을 감싸 공통 응답의 data 구조를 안정적으로 유지한다. */
@Getter
@Builder
@AllArgsConstructor
public class CategoryListResponse {

    private List<CategoryResponse> categories;
}
