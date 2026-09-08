package com.dday.domain.welfare.collector.rule;

/**
 * 룰 하나가 매긴 점수와 그 이유 태그.
 *
 * @param delta 점수 증감. 0이면 이 룰은 판단을 보류한 것 (trace에 남기지 않는다).
 * @param tag   흔적 문자열 (예: {@code R2+2}, {@code R3-2}).
 */
public record RuleHit(int delta, String tag) {

    private static final RuleHit NEUTRAL = new RuleHit(0, "");

    public static RuleHit neutral() {
        return NEUTRAL;
    }

    public static RuleHit of(String rule, int delta) {
        return new RuleHit(delta, rule + (delta >= 0 ? "+" : "") + delta);
    }

    public boolean scored() {
        return delta != 0;
    }
}
