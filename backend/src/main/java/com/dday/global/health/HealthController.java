package com.dday.global.health;

import com.dday.global.common.code.CommonSuccessCode;
import com.dday.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배포된 서버가 살아 있는지, DB까지 닿는지 확인하는 엔드포인트.
 *
 * <p>{@code /health}는 앱만, {@code /health/db}는 커넥션 풀에서 실제 커넥션을 꺼내
 * {@code SELECT 1}을 던진다. <b>배포 직후엔 항상 {@code /health/db}로 확인한다</b> —
 * 앱은 떠 있는데 DB 접속정보나 보안그룹이 틀린 경우를 {@code /health}는 못 잡는다.
 */
@Tag(name = "헬스체크")
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "앱 헬스체크", description = "서버 프로세스가 살아 있는지만 본다. DB는 확인하지 않는다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Void>> health() {
        return ApiResponse.of(CommonSuccessCode.HEALTHY);
    }

    @Operation(summary = "DB 헬스체크", description = "커넥션을 실제로 꺼내 SELECT 1을 던진다. 실패하면 500이 나간다.")
    @GetMapping("/db")
    public ResponseEntity<ApiResponse<Integer>> healthDb() {
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return ApiResponse.of(CommonSuccessCode.DB_HEALTHY, one);
    }
}
