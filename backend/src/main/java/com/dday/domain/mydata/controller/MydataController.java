package com.dday.domain.mydata.controller;

import com.dday.domain.mydata.dto.MydataSuccessCode;
import com.dday.domain.mydata.dto.request.AccountSelectionRequest;
import com.dday.domain.mydata.dto.response.MydataConnectResponse;
import com.dday.domain.mydata.dto.response.MydataSyncResponse;
import com.dday.domain.mydata.dto.response.UserAccountListResponse;
import com.dday.domain.mydata.dto.response.UserAccountResponse;
import com.dday.domain.mydata.service.MydataConnectService;
import com.dday.domain.mydata.service.MydataService;
import com.dday.domain.mydata.service.UserAccountService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "MyData")
@RestController
@RequestMapping("/api/mydata")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MydataController {

    private final MydataService mydataService;
    private final MydataConnectService mydataConnectService;
    private final UserAccountService userAccountService;

    @Operation(summary = "MyData 연동", description = """
            온보딩의 기관 선택 화면이 부른다. 동의 기록 → 데모 목데이터 준비 → 첫 동기화를
            한 번에 끝내므로, 성공하면 바로 소비 내역 화면을 열 수 있다.

            이미 연동한 회원이 다시 불러도 안전하다 — 동의는 갱신되고 거래는 중복 저장되지 않는다.
            """)
    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<MydataConnectResponse>> connect(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(MydataSuccessCode.MYDATA_CONNECTED,
                mydataConnectService.connect(userId));
    }

    @Operation(summary = "MyData 계좌·카드·거래 동기화", description = """
            로그인 회원의 계좌와 카드 정보를 갱신하고 거래를 중복 없이 저장한다.
            from과 to는 함께 전달해야 하며 조회 범위는 from 이상, to 미만이다.
            둘 다 생략하면 현재 월을 포함한 최근 3개월을 동기화한다.
            """)
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<MydataSyncResponse>> sync(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.of(MydataSuccessCode.MYDATA_SYNCED,
                mydataService.sync(userId, from, to));
    }

    @Operation(summary = "연동 계좌 목록 조회", description = """
            마이페이지의 금융정보 관리 화면이 부른다. 온보딩에서 연동한 계좌를 등록 순서대로 준다.
            """)
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<UserAccountListResponse>> getAccounts(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.of(MydataSuccessCode.ACCOUNTS_FOUND, userAccountService.getAccounts(userId));
    }

    @Operation(summary = "계좌 선택 여부 변경", description = """
            이 계좌를 예산 계산에 포함할지 고른다. 계좌 자체를 추가·해지하는 기능은 아니다 —
            그건 `/api/mydata/connect`·`/api/mydata/sync`의 몫이다.

            | HTTP | code | message |
            |---|---|---|
            | 404 | ACCOUNT_NOT_FOUND | 연동된 계좌를 찾을 수 없습니다. |
            """)
    @PatchMapping("/accounts/{accountId}/selection")
    public ResponseEntity<ApiResponse<UserAccountResponse>> updateAccountSelection(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "연동 계좌 ID") @PathVariable Long accountId,
            @Valid @RequestBody AccountSelectionRequest request) {
        return ApiResponse.of(MydataSuccessCode.ACCOUNT_SELECTION_UPDATED,
                userAccountService.updateSelection(userId, accountId, request.getSelected()));
    }
}
