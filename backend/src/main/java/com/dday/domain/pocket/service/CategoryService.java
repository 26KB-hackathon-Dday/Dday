package com.dday.domain.pocket.service;

import com.dday.domain.pocket.dto.PocketErrorCode;
import com.dday.domain.pocket.dto.response.CategoryListResponse;
import com.dday.domain.pocket.dto.response.CategoryResponse;
import com.dday.domain.pocket.entity.Category;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.repository.CategoryRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 활성 카테고리의 평면 조회 결과를 API용 부모-자식 트리로 조립한다. */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 활성 카테고리를 조회한다. 포켓 유형이 없으면 전체를, 있으면 해당 소비 포켓의 카테고리만 반환한다.
     *
     * <p>엔티티 연관에 응답 구조를 직접 의존시키지 않고 DTO 맵을 먼저 만든 뒤 자식을 연결한다.
     * 이 방식은 각 카테고리를 한 번만 변환하면서 부모 탐색도 상수 시간에 처리한다.
     */
    @Transactional(readOnly = true)
    public CategoryListResponse findAll(PocketType pocketType) {
        if (pocketType != null && pocketType != PocketType.ESSENTIAL
                && pocketType != PocketType.FREE) {
            throw new BusinessException(PocketErrorCode.INVALID_POCKET_TYPE);
        }
        List<Category> categories = categoryRepository.findAllActive(pocketType);

        // 1단계: 모든 엔티티를 DTO로 변환해 categoryId로 즉시 찾을 수 있게 한다.
        Map<Long, CategoryResponse> responses = new LinkedHashMap<>();
        categories.forEach(category ->
                responses.put(category.getCategoryId(), CategoryResponse.from(category)));
        // 2단계: 부모가 없는 최상위 카테고리를 응답의 루트 목록으로 잡는다.
        List<CategoryResponse> roots = categories.stream()
                .filter(category -> category.getParentCategory() == null)
                .map(category -> responses.get(category.getCategoryId()))
                .toList();
        // 3단계: 자식 DTO를 이미 만든 부모 DTO의 children에 연결한다.
        categories.stream()
                .filter(category -> category.getParentCategory() != null)
                .forEach(category -> {
                    CategoryResponse parent = responses.get(category.getParentCategory().getCategoryId());
                    if (parent != null) {
                        parent.getChildren().add(responses.get(category.getCategoryId()));
                    }
                });
        return CategoryListResponse.builder().categories(roots).build();
    }
}
