package com.dday.domain.credit.controller;

import com.dday.domain.credit.dto.CreditSuccessCode;
import com.dday.domain.credit.dto.response.ExpectedRateResponse;
import com.dday.domain.credit.service.ExpectedRateService;
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

@Tag(name = "예상 금리")
@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ExpectedRateController {

    private final ExpectedRateService expectedRateService;

    @Operation(summary = "예상 금리 조회", description = """
            로그인한 회원의 **가장 최근 신용점수**로 은행·캐피탈·카드사의 예상 금리를 돌려준다.
            회사 한 곳이 아니라 **업권 평균**이며, **KCB 기준으로 공시한 회사만** 넣었다
            (은행 15곳·캐피탈 8곳·카드사 6곳). NICE는 점수 척도가 달라 섞지 않았다.

            `targetScore`는 현재 점수 + 25점이다 (만점을 넘지 않으며, 이미 만점이면 `null`).
            거기까지 올렸을 때의 금리(`targetRate`)와 `principal`(1,000만원) 기준 연 절약액
            (`annualSaving`)을 함께 준다. 연 이자는 단리 `원금 × 금리 ÷ 100`이다.

            **`null`과 `0`을 구별해야 한다.**
            | 값 | 뜻 |
            |---|---|
            | `currentRate: null` | 그 점수대에 공시가 없다. **금리 0%가 아니다** |
            | `targetScore: null` | 이미 만점이라 더 올릴 여지가 없다 |

            `institutionCount`는 그 점수가 속한 공시 구간의 평균에 들어간 회사 수다.

            **금리 값은 보간해서 만든 값이라 공시된 수치 그대로가 아니다.** 공시 구간이 은행 50점,
            캐피탈·카드사 100점 단위라 구간 값을 그대로 쓰면 25점을 올려도 금리가 그대로인
            점수대가 대부분이다. 공시값을 구간 중앙에 놓고 이웃끼리 직선으로 이었다.

            **기록이 없어도 404가 아니다.** `score: null`, `lenders: []`인 200을 준다.
            """)
    @GetMapping("/rates/expected")
    public ResponseEntity<ApiResponse<ExpectedRateResponse>> findExpected(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(CreditSuccessCode.EXPECTED_RATES_FOUND,
                expectedRateService.findExpected(userId));
    }
}
