package com.dday.domain.pocket.controller;

import com.dday.domain.pocket.dto.PocketSuccessCode;
import com.dday.domain.pocket.dto.response.CategoryListResponse;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.service.CategoryService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "카테고리")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "활성 카테고리 조회", description = """
            활성 상태인 카테고리를 부모-자식 구조로 반환한다.
            pocketType을 생략하면 전체를 조회하고, ESSENTIAL 또는 FREE를 지정하면 해당 유형만 조회한다.
            """)
    @GetMapping
    public ResponseEntity<ApiResponse<CategoryListResponse>> findAll(
            @RequestParam(required = false) PocketType pocketType) {
        return ApiResponse.of(PocketSuccessCode.CATEGORIES_FOUND,
                categoryService.findAll(pocketType));
    }
}
