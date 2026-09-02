package com.dday.domain.welfare.collector.rule;

import java.util.List;

/**
 * Rule 5 — 요약문 안에서 "청년"의 위치 — NOTES.md §7-2, §7-5.
 *
 * <ul>
 *   <li>"청년"이 <b>대상 나열의 일부</b>로 등장 → -1
 *       (예: "청년, (예비)신혼부부, 한부모가족…", "대학생, 청년의…")</li>
 *   <li>요약문이 "청년"으로 <b>시작</b> → +1 (청년이 문장의 주어)</li>
 *   <li>그 외 → 중립</li>
 * </ul>
 *
 * <p>"주어냐 나열이냐"를 형태소 분석 없이 결정론적으로 근사한다: "청년" 앞뒤 15자 안에
 * 구분자({@code , · 및})와 다른 인물명사가 함께 있으면 나열로 본다. 나열 판정이 시작 보너스보다
 * 우선한다(행복주택 요약은 "청년,"으로 시작하지만 나열이다).
 */
public final class Rule5DgstPosition {

    private static final int WINDOW = 15;
    private static final String YOUTH = "청년";
    private static final String SEPARATORS = ",·";

    /** "청년"과 나란히 나오면 나열로 보는 다른 대상 명사들. */
    private static final List<String> OTHER_TARGETS = List.of(
            "신혼부부", "예비", "대학생", "대학원생", "학생", "한부모", "조손", "가족",
            "어르신", "노인", "아동", "청소년", "중장년", "여성", "장애인", "다자녀", "다문화",
            "저소득", "구직자", "경력단절", "근로자", "농업인", "소상공인", "국가유공자"
    );

    private Rule5DgstPosition() {
    }

    public static RuleHit apply(String servDgst) {
        if (servDgst == null || servDgst.isBlank()) {
            return RuleHit.neutral();
        }
        String text = servDgst.trim();

        if (isInEnumeration(text)) {
            return RuleHit.of("R5", -1);
        }
        if (text.startsWith(YOUTH)) {
            return RuleHit.of("R5", 1);
        }
        return RuleHit.neutral();
    }

    private static boolean isInEnumeration(String text) {
        int from = 0;
        while (true) {
            int idx = text.indexOf(YOUTH, from);
            if (idx < 0) {
                return false;
            }
            int start = Math.max(0, idx - WINDOW);
            int end = Math.min(text.length(), idx + YOUTH.length() + WINDOW);
            String window = text.substring(start, end);
            if (hasSeparator(window) && mentionsOtherTarget(window)) {
                return true;
            }
            from = idx + YOUTH.length();
        }
    }

    private static boolean hasSeparator(String window) {
        if (window.contains("및")) {
            return true;
        }
        for (int i = 0; i < window.length(); i++) {
            if (SEPARATORS.indexOf(window.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean mentionsOtherTarget(String window) {
        return OTHER_TARGETS.stream().anyMatch(window::contains);
    }
}
