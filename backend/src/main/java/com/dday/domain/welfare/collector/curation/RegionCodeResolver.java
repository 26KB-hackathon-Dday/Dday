package com.dday.domain.welfare.collector.curation;

import java.util.Map;

/**
 * 제도의 지역명({@code ctpvNm} 시도 / {@code sggNm} 시군구)을 법정동 코드로 바꾼다.
 * 결과는 {@code welfare_program.region_code}에 들어가고, 나중에 유저 거주지와 대조하는 데 쓴다.
 *
 * <p><b>시도 2자리가 기본, 일부 시군구만 10자리.</b> 자격 매칭이 먼저 붙는 용산구·서대문구만
 * {@link #SIGUNGU}에 10자리로 넣고 나머지는 시도로 떨어뜨린다. 대상 구가 늘거나 전국 매핑
 * 테이블이 생기면 {@code refreshRegionCode()}가 매 수집마다 전 행을 다시 파생하므로 자동 반영된다.
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

    /**
     * {@code "시도|시군구"} → 법정동 10자리 코드. 지역 매칭이 시군구 단위로 붙는 구만 넣는다.
     * 여기 없으면 {@link #resolve}는 시도 2자리로 떨어진다.
     *
     * <p><b>프론트 {@code stores/onboarding.ts}의 {@code HARDCODED_REGION_CODES}와 값이 같아야 한다.</b>
     * 유저는 그 코드로, 제도는 이 표로 각각 채워져 자격 판별이 두 값을 접두 비교한다.
     * 전국 법정동 매핑 테이블이 생기면 이 표와 프론트 하드코딩을 둘 다 지운다.
     */
    private static final Map<String, String> SIGUNGU = Map.of(
            "서울특별시|용산구", "1117000000",
            "서울특별시|서대문구", "1141000000");

    private RegionCodeResolver() {
    }

    /**
     * 지역명을 법정동 코드로 푼다. {@link #SIGUNGU}에 있는 시군구면 10자리, 아니면 시도 2자리.
     *
     * @param ctpvNm 시도명. {@code null}이면(CENTRAL/전국) {@code null} 반환
     * @param sggNm  시군구명. {@link #SIGUNGU}에 등록된 곳만 10자리로, 나머지는 무시
     * @return 법정동 코드(2자리 또는 10자리), 또는 못 풀면 {@code null}
     */
    public static String resolve(String ctpvNm, String sggNm) {
        if (ctpvNm == null || ctpvNm.isBlank()) {
            return null;
        }
        String sido = SIDO.get(ctpvNm.strip());
        if (sido == null) {
            return null;
        }
        if (sggNm != null && !sggNm.isBlank()) {
            String sigungu = SIGUNGU.get(ctpvNm.strip() + "|" + sggNm.strip());
            if (sigungu != null) {
                return sigungu;
            }
        }
        return sido;
    }
}
