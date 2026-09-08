package com.dday.global.config;

import com.dday.global.common.dto.ApiResponse;
import com.dday.global.common.jwt.JwtAuthenticationFilter;
import com.dday.global.exception.CommonErrorCode;
import com.dday.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 무상태 인증 설정.
 *
 * <p>CORS는 {@link WebConfig}가 이미 잡아뒀다. {@code cors(withDefaults())}는 별도 빈이 없으면
 * 스프링 MVC의 CORS 설정을 그대로 재사용하므로, <b>허용 출처는 여전히
 * {@code app.cors.allowed-origins} 한 곳에서만 관리한다.</b>
 *
 * <p>인증 실패/거부 응답도 {@code ApiResponse} 봉투로 맞춘다. 스프링 시큐리티는 필터 단계라
 * {@code GlobalExceptionHandler}(컨트롤러 단계)가 잡지 못해서, 여기서 직접 써 준다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** 로그인 없이 열어두는 경로. 인증 API·문서·헬스체크, 그리고 공개 데이터인 지원제도 마스터. */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/**",
            "/health", "/health/**",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/error",
            // 지원제도 마스터는 공개 데이터(공공데이터포털)라 인증이 필요 없다 — 명세서상 /me가 아닌 이유.
            // 컨트롤러에 조회(GET) 엔드포인트만 있다.
            "/api/v1/welfare-programs", "/api/v1/welfare-programs/**",
            // 수집 트리거·리뷰 큐. 컨트롤러가 @Profile("local")이라 운영에는 아예 등록되지 않는다.
            "/internal/welfare/**",
            // 로컬 MyData 연동 검증용 Mock API. 컨트롤러가 local 프로파일에서만 등록된다.
            "/mock/mydata/**",
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 토큰 기반이라 브라우저가 쿠키를 자동으로 실어 보내지 않는다 — CSRF가 성립하지 않는다.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 브라우저 기본 로그인 팝업과 로그인 폼 리다이렉트를 둘 다 끈다. API 서버에는 방해만 된다.
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())

                .exceptionHandling(handling -> handling
                        // 토큰이 없거나 유효하지 않음 → 401
                        .authenticationEntryPoint((request, response, e) ->
                                write(response, CommonErrorCode.UNAUTHORIZED))
                        // 인증은 됐지만 권한 부족 → 403
                        .accessDeniedHandler((request, response, e) ->
                                write(response, CommonErrorCode.FORBIDDEN)))

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(),
                new ApiResponse<>(false, code.name(), code.getMessage(), null, null));
    }

    /** BCrypt. 해시 길이가 항상 60이라 {@code users.password_hash}를 CHAR(60)으로 잡아뒀다. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
