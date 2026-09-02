package com.dday.domain.pocket.controller;

import com.dday.domain.pocket.dto.PocketSuccessCode;
import com.dday.domain.pocket.dto.response.PocketResponse;
import com.dday.domain.pocket.service.PocketService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "포켓")
@RestController
@RequestMapping("/api/pockets")
@RequiredArgsConstructor
public class PocketController {

    private final PocketService pocketService;

    @Operation(summary = "포켓 목록 조회", description = """
            용도별 포켓과 이번 달 소진 현황을 함께 돌려준다.
            정렬은 주거 → 생활 → 비상 → 자산형성 순으로 고정이다.
            """)
    @GetMapping
    public ResponseEntity<ApiResponse<List<PocketResponse>>> findAll() {
        return ApiResponse.of(PocketSuccessCode.POCKETS_FOUND, pocketService.findAll());
    }

    @Operation(summary = "포켓 단건 조회", description = """
            | HTTP | code | message |
            |---|---|---|
            | 404 | POCKET_NOT_FOUND | 포켓을 찾을 수 없습니다. |
            """)
    @GetMapping("/{pocketId}")
    public ResponseEntity<ApiResponse<PocketResponse>> findById(
            @Parameter(description = "포켓 ID") @PathVariable Long pocketId) {
        return ApiResponse.of(PocketSuccessCode.POCKET_FOUND, pocketService.findById(pocketId));
    }
}
