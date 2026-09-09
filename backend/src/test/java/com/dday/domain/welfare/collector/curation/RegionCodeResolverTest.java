package com.dday.domain.welfare.collector.curation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 지역명 → 법정동 코드. 시도 2자리가 기본, 등록된 시군구는 10자리.
 */
class RegionCodeResolverTest {

    @Test
    void 시도명을_2자리_코드로_푼다() {
        assertThat(RegionCodeResolver.resolve("서울특별시", null)).isEqualTo("11");
        assertThat(RegionCodeResolver.resolve("경기도", "수원시")).isEqualTo("41");
        // 등록 안 된 시군구는 시도로 떨어진다
        assertThat(RegionCodeResolver.resolve("서울특별시", "강남구")).isEqualTo("11");
    }

    @Test
    void 등록된_시군구는_10자리_법정동_코드() {
        // 프론트 stores/onboarding.ts HARDCODED_REGION_CODES와 같아야 한다
        assertThat(RegionCodeResolver.resolve("서울특별시", "용산구")).isEqualTo("1117000000");
        assertThat(RegionCodeResolver.resolve("서울특별시", "서대문구")).isEqualTo("1141000000");
    }

    @Test
    void 명칭_변경_전_이름도_현행_코드로_푼다() {
        assertThat(RegionCodeResolver.resolve("강원도", null)).isEqualTo("51");
        assertThat(RegionCodeResolver.resolve("전라북도", null)).isEqualTo("52");
        // 2026-07 광주+전남 통합 → 옛 이름 둘 다 29
        assertThat(RegionCodeResolver.resolve("광주광역시", null)).isEqualTo("29");
        assertThat(RegionCodeResolver.resolve("전라남도", null)).isEqualTo("29");
        assertThat(RegionCodeResolver.resolve("전남광주통합특별시", null)).isEqualTo("29");
    }

    @Test
    void CENTRAL은_null() {
        assertThat(RegionCodeResolver.resolve(null, null)).isNull();
        assertThat(RegionCodeResolver.resolve("", null)).isNull();
    }

    @Test
    void 표에_없는_시도명은_null() {
        assertThat(RegionCodeResolver.resolve("가상특별시", "어딘가구")).isNull();
    }
}
