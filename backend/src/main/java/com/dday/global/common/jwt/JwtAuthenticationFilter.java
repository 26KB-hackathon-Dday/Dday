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
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

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
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
