package com.dday.domain.mydata.controller;

import com.dday.domain.mydata.dto.MydataSuccessCode;
import com.dday.domain.mydata.dto.response.MydataSyncResponse;
import com.dday.domain.mydata.service.MydataService;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "MyData")
@RestController
@RequestMapping("/api/mydata")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MydataController {

    private final MydataService mydataService;

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
}
