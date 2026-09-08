package com.dday.global.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 토큰 발급·검증을 혼자 책임진다. 다른 곳에서 {@code Jwts}를 직접 부르지 않는다.
 *
 * <p>토큰 종류는 {@code type} 클레임으로 구분한다. 이게 없으면 액세스 토큰을 리프레시 자리에
 * 넣어 무한히 갱신할 수 있다 — 수명이 1년이라 특히 위험하다.
 *
 * <p>payload에는 userId만 담는다. 이름·이메일을 넣으면 값이 바뀌어도 토큰 안의 정보는
 * 그대로라 화면에 옛 값이 남는다.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity) {

        this.key = Keys.hmacShaKeyFor(decodeSecret(secret));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    /**
     * 시크릿을 Base64로 먼저 풀어보고, 실패하면 평문 UTF-8 바이트로 쓴다.
     * 운영에서는 Base64 시크릿을 넣는 경우가 많고 로컬 설정은 사람이 읽는 문자열이라
     * 양쪽을 다 받는다. 어느 쪽이든 32바이트 미만이면 {@code Keys}가 기동 중에 예외를 던진다.
     */
    private static byte[] decodeSecret(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (DecodingException | IllegalArgumentException e) {
            // jjwt는 IllegalArgumentException이 아니라 DecodingException을 던진다. 둘 다 받아야
            // 평문 시크릿에서 기동이 깨지지 않는다.
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, TYPE_ACCESS, accessTokenValidity);
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, TYPE_REFRESH, refreshTokenValidity);
    }

    private String createToken(Long userId, String type, long validityMillis) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validityMillis))
                .signWith(key)
                .compact();
    }

    /** 서명·만료가 유효한 액세스 토큰인지. 필터가 매 요청마다 부른다. */
    public boolean validateAccessToken(String token) {
        return parse(token).filter(claims -> TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))).isPresent();
    }

    /**
     * 리프레시 토큰에서 userId를 꺼낸다. 유효하지 않으면 {@code null}이다 —
     * 여기서 예외를 던지지 않는 건 "무엇이 잘못됐는지"를 클라이언트에 알려주지 않기 위해서다.
     */
    public Long parseUserIdFromRefreshToken(String token) {
        return parse(token)
                .filter(claims -> TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class)))
                .map(claims -> Long.valueOf(claims.getSubject()))
                .orElse(null);
    }

    public Long getUserId(String token) {
        return parse(token).map(claims -> Long.valueOf(claims.getSubject())).orElse(null);
    }

    private java.util.Optional<Claims> parse(String token) {
        try {
            return java.util.Optional.of(
                    Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            // 만료·위조는 정상 흐름에서도 흔하다. 스택트레이스 없이 한 줄만 남긴다.
            log.debug("유효하지 않은 토큰: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }
}
