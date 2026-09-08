package com.dday.domain.welfare.client;

import com.dday.domain.welfare.client.dto.WelfareDetailResponse;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상세 응답 파싱 — 중앙·지자체가 같은 루트({@code <wantedDtl>})지만 지원대상·문의처 내부
 * 엘리먼트명이 달라, 양쪽을 다 매핑하고 {@code getXxx()}에서 합치는지 확인한다.
 */
class WelfareDetailResponseParseTest {

    @Test
    void 중앙_상세_지원대상_금액_문의처_홈페이지() throws Exception {
        WelfareDetailResponse d = WelfareXml.readDetailResponse(load("detail-central.xml"));

        assertThat(d.isSuccess()).isTrue();
        assertThat(d.getServId()).isEqualTo("WLF00001175");
        assertThat(d.getAlwServCn()).contains("매월 50만원");
        assertThat(d.getCrtrYr()).isEqualTo("2026");
        // 중앙: tgtrDtlCn
        assertThat(d.targetDescription()).contains("자립준비청년");
        // 중앙 문의처: servSeDetailNm / servSeDetailLink
        assertThat(d.channelName()).isEqualTo("보건복지상담센터");
        assertThat(d.channelPhone()).isEqualTo("129");
        assertThat(d.channelUrl()).isEqualTo("http://www.129.go.kr");
    }

    @Test
    void 지자체_상세_지원대상은_sprtTrgtCn_문의처는_wlfareInfoReld() throws Exception {
        WelfareDetailResponse d = WelfareXml.readDetailResponse(load("detail-local.xml"));

        assertThat(d.getServId()).isEqualTo("WLF00004197");
        assertThat(d.getAlwServCn()).contains("월 300,000원");
        assertThat(d.getCrtrYr()).isNull(); // 지자체엔 없음
        // 지자체: sprtTrgtCn
        assertThat(d.targetDescription()).contains("서대문구");
        // 지자체 문의처: wlfareInfoReldNm / wlfareInfoReldCn
        assertThat(d.channelName()).contains("아동자립지원팀");
        assertThat(d.channelPhone()).isEqualTo("02-330-8680");
        // 지자체 상세엔 inqplHmpgReldList가 없다
        assertThat(d.channelUrl()).isNull();
    }

    private String load(String name) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/welfare/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
