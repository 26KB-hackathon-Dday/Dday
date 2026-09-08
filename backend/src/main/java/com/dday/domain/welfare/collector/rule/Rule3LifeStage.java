package com.dday.domain.welfare.collector.rule;

import java.util.Arrays;
import java.util.List;

/**
 * Rule 3 — 생애주기 구성. {@code lifeArray}(콤마 구분)를 본다 — docs/welfare-api/NOTES.md §7-2.
 *
 * <ul>
 *   <li>{@code [청년]} 단독 → <b>+3</b> (이 사업은 청년만 겨냥한다)</li>
 *   <li>생애주기 3개 이상 → <b>-2</b> (전연령 대상 범용 사업 신호: 행복주택·국민취업 등)</li>
 *   <li>그 외(청년+1개) → 중립</li>
 * </ul>
 *
 * <p>단독 가산점은 원래 +2였는데, 실데이터에서 진짜 청년 사업(청년내일저축계좌 등)이
 * 자동승인 문턱(3)에 못 미쳐 +3으로 올렸다 — NOTES.md §7-5.
 */
public final class Rule3LifeStage {

    private Rule3LifeStage() {
    }

    public static RuleHit apply(String lifeArray) {
        List<String> stages = split(lifeArray);
        if (stages.size() == 1 && stages.get(0).equals("청년")) {
            return RuleHit.of("R3", 3);
        }
        if (stages.size() >= 3) {
            return RuleHit.of("R3", -2);
        }
        return RuleHit.neutral();
    }

    static List<String> split(String lifeArray) {
        if (lifeArray == null || lifeArray.isBlank()) {
            return List.of();
        }
        // 목록 응답은 공백 없는 콤마 구분이지만, 상세 응답은 "청년, 청소년"처럼 공백이 붙는다.
        return Arrays.stream(lifeArray.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
