package com.dday.domain.welfare.collector.curation;

import java.util.Map;

/**
 * 제도의 지역명({@code ctpvNm} 시도 / {@code sggNm} 시군구)을 법정동 코드로 바꾼다.
 * 결과는 {@code welfare_program.region_code}에 들어가고, 나중에 유저 거주지와 대조하는 데 쓴다.
 *
 * <p><b>v1 — 시도 단위(2자리)만.</b> 시군구 5자리 정밀도는 자격 매칭이 실제로 붙을 때 넣는다.
 * 지금은 이 값을 읽는 쪽이 없어서(매칭 미구현), "용산구 사업"이 "서울(11)"로 풀려도 문제가 없다.
 * 분해능을 올리면 {@code refreshRegionCode()}가 매 수집마다 전 행을 다시 파생하므로 자동 반영된다.
 *
 * <p>표에 없는 시도명(오타·신설 미반영)은 {@code null}을 돌려주고, LOCAL 제도가 이러면
 * {@link WelfareIssue#REGION_UNRESOLVED}로 리뷰 큐에 올린다 — 값을 추측해 넣지 않는다.
 *
 * <p>2026-07-01 광주광역시 + 전라남도 → 전남광주통합특별시(29). 옛 이름으로 들어오는
 * 데이터도 있어 legacy 이름을 같이 매핑한다.
 */
public final class RegionCodeResolver {

    /** 시도명 → 법정동 2자리 코드. 현행 16개 + 명칭이 바뀐 4개 legacy. */
    private static final Map<String, String> SIDO = Map.ofEntries(
            Map.entry("서울특별시", "11"),
            Map.entry("부산광역시", "26"),
            Map.entry("대구광역시", "27"),
            Map.entry("인천광역시", "28"),
            Map.entry("전남광주통합특별시", "29"),
            Map.entry("대전광역시", "30"),
            Map.entry("울산광역시", "31"),
            Map.entry("세종특별자치시", "36"),
            Map.entry("경기도", "41"),
            Map.entry("충청북도", "43"),
            Map.entry("충청남도", "44"),
            Map.entry("경상북도", "47"),
            Map.entry("경상남도", "48"),
            Map.entry("제주특별자치도", "50"),
            Map.entry("강원특별자치도", "51"),
            Map.entry("전북특별자치도", "52"),
            // ── 명칭 변경 전 이름 (수집 데이터가 옛 이름을 줄 수 있어 같이 받는다) ──
            Map.entry("광주광역시", "29"),
            Map.entry("전라남도", "29"),
            Map.entry("강원도", "51"),
            Map.entry("전라북도", "52"));

    private RegionCodeResolver() {
    }

    /**
     * @param ctpvNm 시도명. {@code null}이면(CENTRAL/전국) {@code null} 반환
     * @param sggNm  시군구명. v1에서는 쓰지 않는다 (시도 단위 코드만)
     * @return 법정동 2자리 시도 코드, 또는 못 풀면 {@code null}
     */
    public static String resolve(String ctpvNm, String sggNm) {
        if (ctpvNm == null || ctpvNm.isBlank()) {
            return null;
        }
        return SIDO.get(ctpvNm.strip());
    }
}
