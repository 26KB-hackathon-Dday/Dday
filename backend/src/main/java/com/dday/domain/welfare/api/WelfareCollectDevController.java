package com.dday.domain.welfare.api;

import com.dday.domain.welfare.batch.WelfareCollectLauncher;
import com.dday.domain.welfare.dto.WelfareSuccessCode;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발용 수동 트리거. cron(매월 1일)을 기다리지 않고 수집 잡을 바로 돌려본다.
 *
 * <p><b>{@code local} 프로파일에서만 뜬다.</b> 운영에는 노출되지 않는다.
 * 잡은 동기로 실행되므로 응답이 수 초 걸릴 수 있다.
 */
@Profile("local")
@Tag(name = "복지서비스 수집(개발용)")
@RestController
@RequestMapping("/internal/welfare")
@RequiredArgsConstructor
public class WelfareCollectDevController {

    private final WelfareCollectLauncher launcher;

    @Operation(summary = "복지서비스 수집 잡 수동 실행", description = """
            중앙부처·지자체 복지서비스 목록을 긁어와 청년 대상 여부를 룰로 판정하고 welfare_program에 upsert한다.
            local 프로파일 전용. WELFARE_API_KEY가 설정돼 있어야 한다.
            """)
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<Void>> collect() {
        launcher.launch();
        return ApiResponse.of(WelfareSuccessCode.WELFARE_COLLECT_TRIGGERED);
    }
}
