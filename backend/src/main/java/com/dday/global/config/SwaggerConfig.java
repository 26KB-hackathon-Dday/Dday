package com.dday.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: <a href="http://localhost:8080/swagger-ui.html">/swagger-ui.html</a>,
 * OpenAPI JSON: {@code /v3/api-docs}
 *
 * <p>프론트가 경로 목록을 훑는 보조 수단이다. <b>API 계약의 정본은 코드다</b> —
 * 컨트롤러·DTO·{@code ErrorCode} enum이 실제 계약이고, Swagger는 그 사본이다 (AGENTS.md §5).
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI ddayOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Dday API")
                .description("26KB 해커톤 프로젝트 Dday의 백엔드 API")
                .version("v0.0.1"));
    }
}
