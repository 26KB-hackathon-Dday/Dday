package com.dday.domain.welfare.collector.rule;

/**
 * 스코어링 전 사전 필터 — docs/welfare-api/NOTES.md §7-0.
 *
 * <p>Rule 1 키워드가 있거나, {@code "청년"}이 서비스명·요약·소관부서 중 하나에라도 있으면 통과.
 * 아니면 버린다(저장 안 함).
 *
 * <p>소관부서까지 보는 이유: "취업 후 상환 학자금대출"은 요약에 "청년"이 없지만
 * {@code jurOrgNm=청년장학지원과}이고, 상세조회에서 자립준비청년 특례가 드러나는 케이스다.
 * 요약만 봤으면 여기서 죽었다.
 */
public final class YouthPreFilter {

    private static final String YOUTH = "청년";

    private YouthPreFilter() {
    }

    public static boolean passes(String servNm, String servDgst, String jurOrgNm) {
        if (Rule1StrongKeyword.matches(servNm, servDgst)) {
            return true;
        }
        return contains(servNm) || contains(servDgst) || contains(jurOrgNm);
    }

    private static boolean contains(String text) {
        return text != null && text.contains(YOUTH);
    }
}
