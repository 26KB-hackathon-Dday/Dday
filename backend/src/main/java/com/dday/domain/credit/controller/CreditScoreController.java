package com.dday.domain.credit.controller;

import com.dday.domain.credit.dto.CreditSuccessCode;
import com.dday.domain.credit.dto.response.CreditScoreHistoryResponse;
import com.dday.domain.credit.service.CreditScoreService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "신용점수")
@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CreditScoreController {

    private final CreditScoreService creditScoreService;

    @Operation(summary = "최근 신용점수 조회", description = """
            로그인한 회원의 신용점수 기록을 최신순으로 최대 5건 돌려준다.

            각 항목의 `diff`는 **직전 기록 대비 증감**이다. 가장 오래된 항목이라도 비교할 기록이
            서버에 남아 있으면 채워지고, 정말 없을 때만 `null`이다.

            각 항목의 `percentile`은 **상위 몇 %인지**다 (소수점 한 자리). 1000점이면 0.6,
            800점이면 58.3처럼 작을수록 좋다. **KCB(올크레딧) 2025년말 인원분포 기준**이라
            NICE 점수에는 맞지 않는다.

            `latestScore`·`latestPercentile`·`diffFromPrevious`는 `items[0]`의 값과 같다 —
            상단 요약과 그래프가 서로 다른 컴포넌트라 프론트가 매번 첫 원소를 꺼내지 않게 꺼내 뒀다.

            **기록이 없어도 404가 아니다.** 온보딩 직후에는 이력이 없는 게 정상이라
            `latestScore: null`, `latestPercentile: null`, `items: []`인 200을 준다.
            """)
    @GetMapping("/scores/recent")
    public ResponseEntity<ApiResponse<CreditScoreHistoryResponse>> findRecent(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(CreditSuccessCode.CREDIT_SCORES_FOUND,
                creditScoreService.findRecent(userId));
    }
}
