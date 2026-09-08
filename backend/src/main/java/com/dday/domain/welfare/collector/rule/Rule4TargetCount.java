package com.dday.domain.welfare.collector.rule;

import java.util.Arrays;
import java.util.List;

/**
 * Rule 4 — 대상특성 개수. {@code trgterIndvdlArray}의 값이 2개 이상이면 -3 — NOTES.md §7-2.
 *
 * <p>대상특성을 여러 개 붙였다는 건 "저소득·다자녀·장애인·한부모…" 식의 범용 취약계층
 * 프로그램이라는 신호다(통합공공임대가 6개 전부). 0~1개는 판단력이 약해 중립.
 *
 * <p>⚠️ 값 하나 안에 {@code ·}가 들어갈 수 있다({@code 한부모·조손}, {@code 다문화·탈북민}).
 * 그래서 {@code ,}로만 자른다.
 */
public final class Rule4TargetCount {

    private Rule4TargetCount() {
    }

    public static RuleHit apply(String trgterIndvdlArray) {
        if (count(trgterIndvdlArray) >= 2) {
            return RuleHit.of("R4", -3);
        }
        return RuleHit.neutral();
    }

    static int count(String trgterIndvdlArray) {
        if (trgterIndvdlArray == null || trgterIndvdlArray.isBlank()) {
            return 0;
        }
        List<String> values = Arrays.stream(trgterIndvdlArray.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return values.size();
    }
}
