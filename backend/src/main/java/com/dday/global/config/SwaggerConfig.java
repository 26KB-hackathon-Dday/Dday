package com.dday.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
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

    /**
     * Swagger UI 우측 상단 <b>Authorize</b> 버튼이 이 이름을 쓴다.
     * 컨트롤러의 {@code @SecurityRequirement(name = "bearerAuth")}와 문자열이 같아야 한다.
     */
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI ddayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dday API")
                        .description("""
                                26KB 해커톤 프로젝트 Dday의 백엔드 API

                                인증이 필요한 API는 우측 상단 **Authorize**에 `/api/auth/login`으로 받은
                                accessToken을 넣는다. (`Bearer ` 접두사는 Swagger가 알아서 붙인다)
                                """)
                        .version("v0.0.1"))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
