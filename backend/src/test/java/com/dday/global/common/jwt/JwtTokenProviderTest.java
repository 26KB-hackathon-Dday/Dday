package com.dday.global.common.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 시크릿이 짧거나 디코딩에 실패하면 <b>기동 단계에서</b> 죽는다. 그래서 application.yml의
 * 기본 시크릿이 실제로 통과하는지를 여기서 확인한다 — DB 없이 돌릴 수 있는 유일한 기동 검증이다.
 */
class JwtTokenProviderTest {

    /** application.yml의 jwt.secret 기본값과 같아야 한다. 한쪽만 바꾸면 이 테스트가 잡는다. */
    private static final String DEFAULT_SECRET =
            "dday-local-development-only-secret-key-please-override-in-production";

    private static final long ONE_HOUR = 3_600_000L;

    private JwtTokenProvider provider(String secret, long accessValidity) {
        return new JwtTokenProvider(secret, accessValidity, ONE_HOUR);
    }

    @Test
    void application_yml의_기본_시크릿으로_기동할_수_있다() {
        assertThatCode(() -> provider(DEFAULT_SECRET, ONE_HOUR)).doesNotThrowAnyException();
    }

    @Test
    void Base64_시크릿도_평문_시크릿도_받는다() {
        // 운영은 openssl rand -base64 로 만든 값을 넣고, 로컬은 사람이 읽는 문자열을 쓴다.
        String base64 = "c29tZS1yZWFsbHktbG9uZy1iYXNlNjQtc2VjcmV0LWZvci1oczI1Ni10ZXN0aW5n";

        assertThatCode(() -> provider(base64, ONE_HOUR)).doesNotThrowAnyException();
        assertThatCode(() -> provider(DEFAULT_SECRET, ONE_HOUR)).doesNotThrowAnyException();
    }

    @Test
    void 시크릿이_32바이트_미만이면_기동에_실패한다() {
        assertThatCode(() -> provider("too-short", ONE_HOUR)).isInstanceOf(Exception.class);
    }

    @Test
    void 액세스_토큰에서_userId를_꺼낼_수_있다() {
        JwtTokenProvider provider = provider(DEFAULT_SECRET, ONE_HOUR);

        String token = provider.createAccessToken(42L);

        assertThat(provider.validateAccessToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    void 리프레시_토큰은_액세스_토큰_자리에_쓸_수_없다() {
        JwtTokenProvider provider = provider(DEFAULT_SECRET, ONE_HOUR);

        // type 클레임이 없으면 리프레시 토큰으로 무한히 인증을 통과시킬 수 있다.
        assertThat(provider.validateAccessToken(provider.createRefreshToken(42L))).isFalse();
        assertThat(provider.parseUserIdFromRefreshToken(provider.createAccessToken(42L))).isNull();
    }

    @Test
    void 다른_키로_서명된_토큰은_통과하지_못한다() {
        String forged = provider("a-completely-different-secret-key-32-bytes-plus", ONE_HOUR)
                .createAccessToken(42L);

        assertThat(provider(DEFAULT_SECRET, ONE_HOUR).validateAccessToken(forged)).isFalse();
    }

    @Test
    void 만료된_토큰은_통과하지_못한다() {
        // 유효기간을 음수로 주면 발급 즉시 만료된 토큰이 나온다 — Thread.sleep 없이 만료를 검증한다.
        JwtTokenProvider provider = provider(DEFAULT_SECRET, -1000L);

        assertThat(provider.validateAccessToken(provider.createAccessToken(42L))).isFalse();
    }
}
