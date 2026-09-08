package com.dday.domain.pocket.controller;

import com.dday.domain.pocket.dto.PocketSuccessCode;
import com.dday.domain.pocket.dto.response.PocketResponse;
import com.dday.domain.pocket.service.PocketService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "포켓")
@RestController
@RequestMapping("/api/pockets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PocketController {

    private final PocketService pocketService;

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
}
