package com.dday.domain.welfare.client;

import com.dday.domain.welfare.client.dto.LcgvWelfareListItem;
import com.dday.domain.welfare.client.dto.LcgvWelfareListResponse;
import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.entity.AgencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 지자체 목록 응답(docs/welfare-api/list-local.xml)이 우리 DTO로 파싱되고, 공통
 * {@link WelfareListItem}으로 정확히 변환되는지 — NOTES.md §11-3.
 */
class LcgvWelfareListResponseParseTest {

    private LcgvWelfareListResponse response;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/welfare/list-local.xml")) {
            String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            response = WelfareXml.readLcgvListResponse(xml);
        }
    }

    @Test
    void 페이징_메타와_성공여부를_읽는다() {
        assertThat(response.getTotalCount()).isEqualTo(3);
        assertThat(response.getResultCode()).isEqualTo("0");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.servListOrEmpty()).hasSize(3);
    }

    @Test
    void 지역_필드를_읽는다() {
        LcgvWelfareListItem yongsan = byId().get("WLF00006199");
        assertThat(yongsan.getCtpvNm()).isEqualTo("서울특별시");
        assertThat(yongsan.getSggNm()).isEqualTo("용산구");
        assertThat(yongsan.getBizChrDeptNm()).isEqualTo("서울특별시 용산구 생활지원국 아동청소년과");
    }

    @Test
    void 광역_단위_사업은_시군구가_없다() {
        // 전남광주통합특별시 자립정착금엔 <sggNm> 엘리먼트 자체가 없다.
        assertThat(byId().get("WLF00001803").getSggNm()).isNull();
        assertThat(byId().get("WLF00001803").getCtpvNm()).isEqualTo("전남광주통합특별시");
    }

    @Test
    void 공통_DTO로_변환하면_LOCAL로_표시되고_콤마뒤_공백이_제거된다() {
        WelfareListItem common = byId().get("WLF00004197").toCommon();

        assertThat(common.getAgencyType()).isEqualTo(AgencyType.LOCAL);
        // 원본은 "아동, 청소년, 청년" — CENTRAL 포맷(공백 없음)으로 통일
        assertThat(common.getLifeArray()).isEqualTo("아동,청소년,청년");
        assertThat(common.getIntrsThemaArray()).isEqualTo("보호·돌봄,서민금융");
        // bizChrDeptNm을 jurOrgNm 자리에도 넣는다 (Rule 2 입력)
        assertThat(common.getJurOrgNm()).isEqualTo("서울특별시 서대문구 교육문화체육국 아동청소년과");
        assertThat(common.getCtpvNm()).isEqualTo("서울특별시");
        assertThat(common.getSggNm()).isEqualTo("서대문구");
        assertThat(common.getLastModYmd()).isEqualTo("20260717");
        // LOCAL엔 대상특성이 없다
        assertThat(common.getTrgterIndvdlArray()).isNull();
    }

    private Map<String, LcgvWelfareListItem> byId() {
        return response.servListOrEmpty().stream()
                .collect(Collectors.toMap(LcgvWelfareListItem::getServId, Function.identity()));
    }
}
