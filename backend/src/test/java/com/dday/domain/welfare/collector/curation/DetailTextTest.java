package com.dday.domain.welfare.collector.curation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DetailTextTest {

    @Test
    void 줄바꿈과_중복공백을_접는다() {
        assertThat(DetailText.cleanTarget("보호종료 5년 이내\r\n  아동 중  서대문구 거주자"))
                .isEqualTo("보호종료 5년 이내 아동 중 서대문구 거주자");
    }

    @Test
    void 각주는_이제_건드리지_않는다() {
        // 결정론 규칙으로 관공서 자유서술을 자르지 않는다 — 원문 그대로 통과, 정리는 후속 LLM 몫.
        assertThat(DetailText.cleanTarget(
                "만 19세 ~ 34세 청년에게 지원합니다. ※ 개인회생, 개인워크아웃 성실상환자"))
                .isEqualTo("만 19세 ~ 34세 청년에게 지원합니다. ※ 개인회생, 개인워크아웃 성실상환자");
    }

    @Test
    void 오백자를_넘으면_자른다() {
        String longText = "가".repeat(600);
        assertThat(DetailText.cleanTarget(longText)).hasSize(500);
    }

    @Test
    void 빈값() {
        assertThat(DetailText.cleanTarget(null)).isNull();
        assertThat(DetailText.cleanTarget("  ")).isNull();
    }

    @Test
    void 스킴_없는_URL엔_https를_붙인다() {
        assertThat(DetailText.normalizeUrl("www.kinfa.or.kr")).isEqualTo("https://www.kinfa.or.kr");
        assertThat(DetailText.normalizeUrl("  www.kosaf.go.kr ")).isEqualTo("https://www.kosaf.go.kr");
    }

    @Test
    void 이미_스킴이_있으면_그대로() {
        assertThat(DetailText.normalizeUrl("https://www.molit.go.kr/")).isEqualTo("https://www.molit.go.kr/");
        assertThat(DetailText.normalizeUrl("HTTP://www.129.go.kr")).isEqualTo("HTTP://www.129.go.kr");
    }

    @Test
    void 프로토콜_상대경로는_https로() {
        assertThat(DetailText.normalizeUrl("//example.com/a")).isEqualTo("https://example.com/a");
    }

    @Test
    void 점이_없으면_URL로_안_본다() {
        assertThat(DetailText.normalizeUrl("129")).isNull();
        assertThat(DetailText.normalizeUrl("전화문의")).isNull();
    }

    @Test
    void URL_빈값() {
        assertThat(DetailText.normalizeUrl(null)).isNull();
        assertThat(DetailText.normalizeUrl("   ")).isNull();
    }
}
