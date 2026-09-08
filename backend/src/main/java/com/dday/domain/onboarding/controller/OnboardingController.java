package com.dday.domain.onboarding.controller;

import com.dday.domain.onboarding.dto.OnboardingSuccessCode;
import com.dday.domain.onboarding.dto.request.*;
import com.dday.domain.onboarding.dto.response.*;
import com.dday.domain.onboarding.service.OnboardingService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 온보딩. 전부 로그인이 필요하다({@code SecurityConfig}의 {@code anyRequest().authenticated()}).
 *
 * <p><b>경로에 회원 id가 없다.</b> {@code userId}는 {@code JwtAuthenticationFilter}가 토큰에서
 * 꺼내 앉혀둔 값을 받는다 — 본문이나 경로로 받으면 남의 온보딩을 채울 수 있다.
 *
 * <p>화면 하나에 API 하나다. 마지막에 몰아서 저장하면 중간에 이탈했을 때 입력이 통째로 날아간다.
 */
@Tag(name = "온보딩")
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(summary = "온보딩 진행 상태 조회", description = """
            지금은 완료 여부만 준다. 단계 저장 컬럼이 없어 "이어하기"는 지원하지 않는다.
            """)
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<OnboardingProgressResponse>> getProgress(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(OnboardingSuccessCode.ONBOARDING_PROGRESS_FOUND,
                onboardingService.getProgress(userId));
    }

    @Operation(summary = "보호종료일 저장", description = """
            저장과 동시에 지원 종료일(보호종료일 + 5년)·D-day·남은 개월 수를 계산해 함께 준다.

            `protectionStatus`는 보호종료일이 미래면 `IN_PROTECTION`, 오늘이거나 과거면 `DISCHARGED`다.
            지원 기간이 이미 지났으면 `dDay`·`remainingMonths`가 **음수**로 나간다.
            """)
    @PostMapping("/protection-date")
    public ResponseEntity<ApiResponse<ProtectionDateResponse>> saveProtectionDate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProtectionDateRequest request) {
        return ApiResponse.of(OnboardingSuccessCode.PROTECTION_DATE_SAVED,
                onboardingService.saveProtectionDate(userId, request));
    }

    @Operation(summary = "거주지역 저장")
    @PostMapping("/region")
    public ResponseEntity<ApiResponse<Void>> saveRegion(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RegionRequest request) {
        onboardingService.saveRegion(userId, request);
        return ApiResponse.of(OnboardingSuccessCode.REGION_SAVED);
    }

    @Operation(summary = "주거형태 저장", description = """
            `housingType`: DORM · ETC · FAMILY · LH_JEONSE · MONTHLY · SELF_RELIANCE_HOUSE

            `skipHousingCost`가 true면 프론트가 주거비 입력 화면을 건너뛴다.
            현재 enum에는 시설 보호 유형이 없어 **항상 false**다.
            """)
    @PostMapping("/housing-type")
    public ResponseEntity<ApiResponse<HousingTypeResponse>> saveHousingType(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody HousingTypeRequest request) {
        return ApiResponse.of(OnboardingSuccessCode.HOUSING_TYPE_SAVED,
                onboardingService.saveHousingType(userId, request));
    }

    @Operation(summary = "주거비 저장", description = """
            세 값 모두 선택 입력이다. **보내지 않은 값은 지워진다** — 부분 수정이 아니라 통째로 덮어쓴다.

            `estimatedMonthly`(월세 + 관리비)는 **DB에 저장하지 않고 매번 계산한** 값이다.
            월세·관리비가 둘 다 없으면 0이 아니라 `null`이다(= 해당 없음).
            """)
    @PostMapping("/housing-cost")
    public ResponseEntity<ApiResponse<HousingCostSaveResponse>> saveHousingCost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody HousingCostRequest request) {
        return ApiResponse.of(OnboardingSuccessCode.HOUSING_COST_SAVED,
                onboardingService.saveHousingCost(userId, request));
    }

    @Operation(summary = "정기수입 목록 조회")
    @GetMapping("/incomes")
    public ResponseEntity<ApiResponse<IncomeListResponse>> getIncomes(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(OnboardingSuccessCode.INCOMES_FOUND,
                onboardingService.getIncomes(userId));
    }

    @Operation(summary = "정기수입 추가", description = """
            `incomeType`: SALARY · ALLOWANCE · SETTLEMENT_FUND · DIDIM_SEED · ETC

            저장 후 합계를 다시 계산해 함께 준다 — 프론트가 목록을 다시 부르지 않아도 된다.
            """)
    @PostMapping("/incomes")
    public ResponseEntity<ApiResponse<IncomeSaveResponse>> addIncome(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody IncomeRequest request) {
        return ApiResponse.of(OnboardingSuccessCode.INCOME_CREATED,
                onboardingService.addIncome(userId, request));
    }

    @Operation(summary = "정기수입 삭제", description = """
            본인 것만 지울 수 있다. 남의 수입이든 없는 id든 **같은 404**를 준다
            (구분해 주면 어떤 id가 존재하는지 알려주는 꼴이 된다).

            | HTTP | code | message |
            |---|---|---|
            | 404 | INCOME_NOT_FOUND | 정기수입을 찾을 수 없습니다. |
            """)
    @DeleteMapping("/incomes/{incomeId}")
    public ResponseEntity<ApiResponse<IncomeDeleteResponse>> deleteIncome(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "정기수입 ID") @PathVariable Long incomeId) {
        return ApiResponse.of(OnboardingSuccessCode.INCOME_DELETED,
                onboardingService.deleteIncome(userId, incomeId));
    }

    @Operation(summary = "자산 정보 저장", description = """
            `settlementReceived`: RECEIVED(받았어요) · NOT_YET(아직) · NONE(해당없음)
            """)
    @PostMapping("/assets")
    public ResponseEntity<ApiResponse<Void>> saveAssets(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AssetRequest request) {
        onboardingService.saveAssets(userId, request);
        return ApiResponse.of(OnboardingSuccessCode.ASSETS_SAVED);
    }

    @Operation(summary = "온보딩 완료", description = """
            완료 표시 후 첫 자립계획 요약을 준다.

            보호종료일을 건너뛴 회원도 완료할 수 있고, 그때는 `dDay`가 `null`이다.
            `monthlyExpense`는 현재 주거비(월세 + 관리비)만 센다 — 예산 초안이 붙으면 그 지출이 더해진다.
            """)
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<OnboardingCompleteResponse>> complete(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(OnboardingSuccessCode.ONBOARDING_COMPLETED,
                onboardingService.complete(userId));
    }
}
