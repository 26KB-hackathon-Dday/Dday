package com.dday.domain.pocket.controller;

import com.dday.domain.pocket.dto.PocketSuccessCode;
import com.dday.domain.pocket.dto.response.PocketResponse;
import com.dday.domain.pocket.dto.request.PocketUpdateRequest;
import com.dday.domain.pocket.dto.response.PocketInitializeResponse;
import com.dday.domain.pocket.dto.response.PocketMonthlyResponse;
import com.dday.domain.pocket.dto.response.TransactionListItemResponse;
import com.dday.domain.mydata.entity.ClassificationStatus;
import com.dday.domain.pocket.entity.PocketType;
import com.dday.domain.pocket.service.PocketService;
import com.dday.domain.pocket.service.TransactionQueryService;
import com.dday.global.common.dto.ApiResponse;
import com.dday.global.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.Valid;

@Tag(name = "포켓")
@RestController
@RequestMapping("/api/pockets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PocketController {

    private final PocketService pocketService;
    private final TransactionQueryService transactionQueryService;

    @Operation(summary = "기본 포켓 초기화", description = """
            로그인한 회원에게 없는 기본 포켓만 생성한다.
            이미 생성된 포켓은 유지되므로 여러 번 호출해도 중복 행이 생기지 않는다.
            """)
    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<PocketInitializeResponse>> initialize(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(PocketSuccessCode.POCKETS_INITIALIZED,
                pocketService.initialize(userId));
    }

    @Operation(summary = "월별 포켓 현황 조회", description = """
            month는 yyyy-MM 형식이다.
            포켓별 목표액과 해당 월의 정상 소비 거래를 집계해 사용액·잔액·초과액·사용률을 반환한다.
            """)
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<PocketMonthlyResponse>> findMonthly(
            @AuthenticationPrincipal Long userId,
            @RequestParam String month) {
        return ApiResponse.of(PocketSuccessCode.MONTHLY_POCKETS_FOUND,
                pocketService.findMonthly(userId, month));
    }

    @Operation(summary = "포켓별 거래 목록 조회", description = """
            필수, 자유 또는 비상금 포켓의 거래를 월 단위로 조회한다.
            categoryId와 classificationStatus는 선택 필터이며, page는 0부터 시작하고 size는 최대 100이다.
            결과는 거래 시각과 거래 ID의 내림차순으로 정렬된다.
            """)
    @GetMapping("/{pocketType}/transactions")
    public ResponseEntity<ApiResponse<PageResponse<TransactionListItemResponse>>> findTransactions(
            @AuthenticationPrincipal Long userId,
            @PathVariable PocketType pocketType,
            @RequestParam String month,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ClassificationStatus classificationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.of(PocketSuccessCode.POCKET_TRANSACTIONS_FOUND,
                transactionQueryService.findAll(userId, pocketType, month, categoryId,
                        classificationStatus, page, size));
    }

    @Operation(summary = "내 포켓 목록 조회", description = """
            로그인한 회원의 포켓 네 개를 돌려준다.
            정렬은 필수 → 자유 → 비상금 → 미래자산 순으로 고정이다.

            **금액은 여기 없다.** 목표 예산은 예산 API가, 소진액은 소비 조회 API가 내려준다.
            """)
    @GetMapping
    public ResponseEntity<ApiResponse<List<PocketResponse>>> findAll(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(PocketSuccessCode.POCKETS_FOUND, pocketService.findAll(userId));
    }

    @Operation(summary = "포켓 단건 조회", description = """
            | HTTP | code | message |
            |---|---|---|
            | 401 | UNAUTHORIZED | 로그인이 필요합니다. |
            | 404 | POCKET_NOT_FOUND | 포켓을 찾을 수 없습니다. |

            내 포켓이 아니면 404다. 403으로 답하면 그 id의 포켓이 있다는 사실이 새어나간다.
            """)
    @GetMapping("/{pocketId}")
    public ResponseEntity<ApiResponse<PocketResponse>> findById(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "포켓 ID") @PathVariable Long pocketId) {
        return ApiResponse.of(PocketSuccessCode.POCKET_FOUND,
                pocketService.findById(userId, pocketId));
    }

    @Operation(summary = "포켓 정보 수정", description = """
            필수·자유·비상금 포켓의 이름과 설명을 부분 수정한다.
            보내지 않은 필드는 기존 값을 유지하며 포켓 유형과 월별 예산은 변경하지 않는다.
            미래자산 포켓은 이 API의 수정 대상이 아니다.
            """)
    @PatchMapping("/{pocketId}")
    public ResponseEntity<ApiResponse<PocketResponse>> update(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "포켓 ID") @PathVariable Long pocketId,
            @Valid @RequestBody PocketUpdateRequest request) {
        return ApiResponse.of(PocketSuccessCode.POCKET_UPDATED,
                pocketService.update(userId, pocketId, request));
    }
}
