package com.dday.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 프론트엔드가 다른 포트/도메인에서 뜨므로 CORS를 열어준다.
 *
 * <p>허용 출처는 프로파일별 설정의 {@code app.cors.allowed-origins}에서 온다
 * (쉼표로 구분). 프론트 dev 서버 포트가 바뀌면 코드가 아니라 그 값을 고친다.
 *
 * <p>{@code allowedOrigins}가 아니라 {@code allowedOriginPatterns}를 쓰는 이유:
 * {@code allowCredentials(true)}와 와일드카드 {@code *}는 같이 못 쓴다 — 스프링이 기동 중에
 * 예외를 던진다. 패턴 API는 그 조합을 허용한다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
