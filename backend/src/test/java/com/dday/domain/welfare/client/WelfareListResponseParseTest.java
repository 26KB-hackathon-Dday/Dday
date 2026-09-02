package com.dday.domain.welfare.client;

import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.client.dto.WelfareListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 응답 XML(docs/welfare-api/list-central.xml 사본)이 우리 DTO로 정확히 파싱되는지.
 * 이 API는 표준 data.go.kr 봉투가 아니라 {@code <wantedList>} 고유 구조라 회귀 위험이 있다.
 */
class WelfareListResponseParseTest {

    private WelfareListResponse response;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/welfare/list-central.xml")) {
            String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            response = WelfareXml.readListResponse(xml);
        }
    }

    @Test
    void 페이징_메타와_성공여부를_읽는다() {
        assertThat(response.getTotalCount()).isEqualTo(28);
        assertThat(response.getPageNo()).isEqualTo(1);
        assertThat(response.getNumOfRows()).isEqualTo(10);
        assertThat(response.getResultCode()).isEqualTo("0");
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void 항목_열개가_래퍼없이_반복_파싱된다() {
        assertThat(response.servListOrEmpty()).hasSize(10);
    }

    @Test
    void 첫_항목의_필드가_그대로_들어온다() {
        WelfareListItem first = response.servListOrEmpty().get(0);
        assertThat(first.getServId()).isEqualTo("WLF00004661");
        assertThat(first.getServNm()).isEqualTo("청년월세 지원사업");
        assertThat(first.getJurOrgNm()).isEqualTo("청년주거정책과");
        assertThat(first.getLifeArray()).isEqualTo("청년");
        assertThat(first.getTrgterIndvdlArray()).isEqualTo("저소득");
        assertThat(first.getSvcfrstRegTs()).isEqualTo("20220413");
    }

    @Test
    void 대상특성_엘리먼트가_없으면_null이다() {
        // 햇살론youth 항목엔 <trgterIndvdlArray> 자체가 없다.
        assertThat(byId().get("WLF00001076").getTrgterIndvdlArray()).isNull();
    }

    @Test
    void 값_안에_가운뎃점이_있는_대상특성은_통째로_한_값이다() {
        assertThat(byId().get("WLF00004649").getTrgterIndvdlArray()).isEqualTo("저소득,한부모·조손");
    }

    private Map<String, WelfareListItem> byId() {
        return response.servListOrEmpty().stream()
                .collect(Collectors.toMap(WelfareListItem::getServId, Function.identity()));
    }
}
