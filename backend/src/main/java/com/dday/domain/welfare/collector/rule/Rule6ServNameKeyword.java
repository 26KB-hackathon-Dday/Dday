package com.dday.domain.welfare.collector.rule;

/**
 * Rule 6 — 서비스명에 "청년"이 박혀 있으면 +2.
 *
 * <p>서비스명은 요약(servDgst)과 달리 대상을 나열하지 않는다. 이름에 "청년"이 들어갔다는 건
 * ("청년창업농장학금", "청년내일채움공제") 그 사업이 청년을 겨냥해 만들어졌다는 뜻이다.
 *
 * <p>이 룰이 없으면 "청년창업농장학금 지원"이 걸러진다 — 요약엔 "농업 후계인력"만 있고
 * lifeArray가 청년·중장년·노년 3개라 Rule 3에서 -2를 맞기 때문. docs/welfare-api/NOTES.md §7-2.
 */
public final class Rule6ServNameKeyword {

    private Rule6ServNameKeyword() {
    }

    public static RuleHit apply(String servNm) {
        if (servNm != null && servNm.contains("청년")) {
            return RuleHit.of("R6", 2);
        }
        return RuleHit.neutral();
    }
}
