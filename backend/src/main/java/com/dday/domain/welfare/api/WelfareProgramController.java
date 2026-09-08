package com.dday.domain.welfare.api;

import com.dday.domain.welfare.dto.WelfareSuccessCode;
import com.dday.domain.welfare.dto.response.WelfareProgramDetailResponse;
import com.dday.domain.welfare.dto.response.WelfareProgramSummaryResponse;
import com.dday.domain.welfare.service.WelfareProgramService;
import com.dday.global.common.dto.ApiResponse;
import com.dday.global.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전체 지원제도 검색·상세 (docs/welfare-api/matching-spec.md — SUBSIDY-009 / SUBSIDY-005).
 *
 * <p>제도 마스터는 공개 데이터라 인증이 없다. {@code /me} 엔드포인트가 아니다.
 */
@Tag(name = "지원제도")
@RestController
@RequestMapping("/api/v1/welfare-programs")
@RequiredArgsConstructor
public class WelfareProgramController {

    private final WelfareProgramService welfareProgramService;

    @Operation(summary = "전체 지원제도 검색·목록", description = """
            `q`(검색어)와 `category`(카테고리)는 서로 독립된 필터다. 둘 다 주면 AND, 없으면 전체.
            `benefitText`·`periodText`·`status`는 서버가 파생해 내려준다.
            """)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WelfareProgramSummaryResponse>>> search(
            @Parameter(description = "검색어 — 제도명·소관부처·지원대상 부분일치")
            @RequestParam(required = false) String q,
            @Parameter(description = "카테고리 정확일치 (주거·생활·서민금융·교육·일자리·기타 …)")
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.of(
                WelfareSuccessCode.WELFARE_PROGRAMS_FOUND,
                welfareProgramService.search(q, category, pageable));
    }

    @Operation(summary = "제도별 신청 안내 상세", description = """
            | HTTP | code | message |
            |---|---|---|
            | 404 | PROGRAM_NOT_FOUND | 존재하지 않는 지원제도입니다. |
            """)
    @GetMapping("/{programId}")
    public ResponseEntity<ApiResponse<WelfareProgramDetailResponse>> getById(
            @Parameter(description = "제도 ID (= servId, 예: WLF00004661)")
            @PathVariable String programId) {
        return ApiResponse.of(
                WelfareSuccessCode.WELFARE_PROGRAM_FOUND,
                welfareProgramService.getByProgramId(programId));
    }
}
