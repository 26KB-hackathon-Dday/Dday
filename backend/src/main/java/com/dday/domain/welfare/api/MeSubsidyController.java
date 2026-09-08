package com.dday.domain.welfare.api;

import com.dday.domain.welfare.dto.WelfareSuccessCode;
import com.dday.domain.welfare.dto.request.ReceivingStatusUpdateRequest;
import com.dday.domain.welfare.dto.response.ReceivingStatusResponse;
import com.dday.domain.welfare.dto.response.SubsidyHomeResponse;
import com.dday.domain.welfare.service.SubsidyEligibilityService;
import com.dday.domain.welfare.service.SubsidyHomeService;
import com.dday.domain.welfare.service.SubsidyStatusService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 지원금 현황 (SUBSIDY-002/003/004).
 *
 * <p>{@code /me} 패턴 — 인증 토큰에서 유저를 특정하므로 path에 userId를 노출하지 않는다.
 */
@Tag(name = "내 지원금")
@RestController
@RequestMapping("/api/v1/me/subsidies")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MeSubsidyController {

    private final SubsidyHomeService subsidyHomeService;
    private final SubsidyStatusService subsidyStatusService;
    private final SubsidyEligibilityService subsidyEligibilityService;

    @Operation(summary = "지원금 매칭 홈", description = """
            자격 있는 제도(확인된 지원)를 수급 상태로 나눠 준다 — 받고 있는 / 놓치고 있을 수 있는 /
            확인 필요한. 판별 이력이 없으면 이 호출이 먼저 자격 판별을 실행한다.
            """)
    @GetMapping("/home")
    public ResponseEntity<ApiResponse<SubsidyHomeResponse>> home(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(WelfareSuccessCode.SUBSIDY_HOME_FOUND, subsidyHomeService.getHome(userId));
    }

    @Operation(summary = "수급 여부 등록/수정", description = """
            | HTTP | code | 상황 |
            |---|---|---|
            | 422 | INELIGIBLE_PROGRAM | 자격이 확인되지 않은 제도에 상태 등록 시도 |
            """)
    @PatchMapping("/{programId}/receiving-status")
    public ResponseEntity<ApiResponse<ReceivingStatusResponse>> updateReceivingStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable String programId,
            @Valid @RequestBody ReceivingStatusUpdateRequest request) {
        return ApiResponse.of(WelfareSuccessCode.RECEIVING_STATUS_UPDATED,
                subsidyStatusService.updateReceiving(userId, programId, request.getReceivingStatus()));
    }

    @Operation(summary = "자격 재판별", description = "프로필·마이데이터 변경 후 자격을 다시 계산한다.")
    @PostMapping("/eligibility/refresh")
    public ResponseEntity<ApiResponse<SubsidyHomeResponse>> refreshEligibility(
            @AuthenticationPrincipal Long userId) {
        subsidyEligibilityService.evaluate(userId);
        return ApiResponse.of(WelfareSuccessCode.ELIGIBILITY_REEVALUATED, subsidyHomeService.getHome(userId));
    }
}
