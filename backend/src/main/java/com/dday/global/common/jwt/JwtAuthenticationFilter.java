package com.dday.global.common.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * {@code Authorization: Bearer {accessToken}}을 검사해 SecurityContext에 userId를 앉힌다.
 *
 * <p><b>토큰이 없거나 틀려도 여기서 응답을 만들지 않는다.</b> 그냥 인증 없이 통과시키고,
 * 접근 거부 여부는 뒤의 인가 단계가 판단한다. 이렇게 해야 permitAll 경로(/api/auth/**)가
 * 잘못된 토큰을 달고 와도 정상 동작한다.
 *
 * <p>컨트롤러에서는 {@code @AuthenticationPrincipal Long userId}로 꺼내 쓰면 된다.
 *
 * <p><b>SSE 구독 경로만 예외로 쿼리 파라미터 토큰도 받는다.</b> 브라우저 네이티브
 * {@code EventSource}는 커스텀 헤더를 못 실어 보내서, 헤더 인증만 있으면 알림 구독을
 * 열 방법이 없다. 그렇다고 전체 API에 쿼리 파라미터 토큰을 허용하면 URL에 토큰이 남아
 * 접근 로그·프록시·브라우저 히스토리에 노출된다 — 그래서 이 경로 하나로 좁힌다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final String TOKEN_QUERY_PARAM = "token";

    /** 쿼리 파라미터 토큰을 예외적으로 허용하는 경로. 늘어나면 배열로 바꾼다. */
    private static final String SSE_SUBSCRIBE_PATH = "/api/notifications/subscribe";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        // 이미 인증된 컨텍스트를 덮어쓰지 않는다(다른 필터가 앉혔을 수 있다).
        if (token != null
                && SecurityContextHolder.getContext().getAuthentication() == null
                && jwtTokenProvider.validateAccessToken(token)) {

            Long userId = jwtTokenProvider.getUserId(token);
            var authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length()).trim();
            if (!token.isEmpty()) return token;
        }

        if (SSE_SUBSCRIBE_PATH.equals(request.getRequestURI())) {
            String queryToken = request.getParameter(TOKEN_QUERY_PARAM);
            if (queryToken != null && !queryToken.isBlank()) return queryToken;
        }

        return null;
    }
}
