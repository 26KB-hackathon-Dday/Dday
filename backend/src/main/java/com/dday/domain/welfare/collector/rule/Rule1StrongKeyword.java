package com.dday.domain.welfare.collector.rule;

import java.util.List;

/**
 * Rule 1 — 자립준비청년 전용 키워드. 매칭되면 스코어 계산 없이 {@code STRONG_YOUTH} 확정.
 *
 * <p>키워드는 {@code 자립준비청년}, {@code 보호종료} 둘뿐이다. {@code 가정위탁}은 제외했다
 * (위탁가정 일반 지원까지 딸려 와서) — docs/welfare-api/NOTES.md §7-1.
 */
public final class Rule1StrongKeyword {

    /** 목록 단계에서 보는 필드. 상세보강(Step 2)이 붙으면 tgtrDtlCn 등도 함께 본다. */
    private static final List<String> KEYWORDS = List.of("자립준비청년", "보호종료");

    private Rule1StrongKeyword() {
    }

    public static boolean matches(String... texts) {
        for (String text : texts) {
            if (text == null) {
                continue;
            }
            for (String keyword : KEYWORDS) {
                if (text.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }
}
