package com.dday.domain.welfare.api;

import com.dday.domain.welfare.dto.WelfareSuccessCode;
import com.dday.domain.welfare.dto.response.WelfareReviewItemResponse;
import com.dday.domain.welfare.service.WelfareProgramService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 큐레이션 리뷰 큐 (관리자용). 검증에서 걸린 제도({@code curation_status = NEEDS_REVIEW})를
 * 사유·현재 값과 함께 보여준다.
 *
 * <p><b>{@code local} 프로파일에서만 뜬다.</b> 시큐리티가 없어 운영 노출은 아직 안 한다.
 * 교정은 DB에서 직접 하고, 끝나면 {@code source = 'MANUAL_CURATION'}으로 바꿔 동결한다
 * (재수집이 큐레이션 필드를 덮지 않게 됨). 되돌리려면 {@code 'API_CANDIDATE'}로.
 */
@Profile("local")
@Tag(name = "복지서비스 리뷰 큐(관리자용)")
@RestController
@RequestMapping("/internal/welfare")
@RequiredArgsConstructor
public class WelfareReviewController {

    private final WelfareProgramService welfareProgramService;

    @Operation(summary = "리뷰 큐 조회", description = """
            검증에서 걸렸고(`NEEDS_REVIEW`) 아직 손 안 댄(`source = API_CANDIDATE`) 제도 목록.
            각 항목에 걸린 사유(`issues`)와 문제 필드의 현재 값을 함께 준다.
            원문은 `welfare_program.raw_list_xml` / `raw_detail_xml` 컬럼, 복지로 원본은 `detailLink`.
            """)
    @GetMapping("/review-queue")
    public ResponseEntity<ApiResponse<List<WelfareReviewItemResponse>>> reviewQueue() {
        return ApiResponse.of(
                WelfareSuccessCode.WELFARE_REVIEW_QUEUE_FOUND,
                welfareProgramService.getReviewQueue());
    }
}
