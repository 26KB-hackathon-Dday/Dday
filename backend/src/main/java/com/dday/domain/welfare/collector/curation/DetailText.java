package com.dday.domain.welfare.collector.curation;

/**
 * 상세 API의 자연어 필드를 화면에 쓸 만하게 다듬는다.
 */
public final class DetailText {

    private static final int TARGET_MAX = 500;

    private DetailText() {
    }

    /**
     * 지원대상({@code tgtrDtlCn}/{@code sprtTrgtCn}) 정리 — 줄바꿈·중복 공백만 접고 길이를 제한한다.
     *
     * <p>이전엔 {@code *}·{@code ※} 각주를 잘라내는 정규식을 뒀는데, 표본 2~3건에 맞춘 규칙이라
     * 형태가 다른 원문(안내문이 앞에 오는 청년월세 등)에서 자격요건을 통째로 날렸다. 관공서
     * 자유서술을 결정론 규칙으로 "고치는" 건 포기하고, 원문을 그대로 통과시킨다. 정리가 필요한
     * 건은 검증({@code WelfareProgramValidator})이 {@code NEEDS_REVIEW}로 잡고, 실제 정돈은
     * 후속 LLM 큐레이션이 맡는다.
     */
    public static String cleanTarget(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.replaceAll("\\s+", " ").trim();
        return s.length() > TARGET_MAX ? s.substring(0, TARGET_MAX) : s;
    }

    /**
     * 관련 홈페이지 URL 정리. data.go.kr 상세는 스킴을 빼먹고 주기도 한다
     * ({@code www.kinfa.or.kr}, {@code www.kosaf.go.kr}). 그대로 링크에 걸면 브라우저가
     * 현재 사이트 기준 상대경로로 해석해 404가 난다 — 스킴이 없으면 {@code https://}를 붙인다.
     *
     * <pre>
     * "www.kinfa.or.kr"        → "https://www.kinfa.or.kr"
     * "https://www.molit.go.kr/" → 그대로
     * "//example.com"          → "https://example.com"
     * "129"                    → null  (점이 없으면 URL로 안 본다)
     * </pre>
     */
    public static String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.startsWith("//")) {
            s = "https:" + s;
        } else if (!s.matches("(?i)^https?://.*")) {
            if (!s.contains(".")) {
                return null;
            }
            s = "https://" + s;
        }
        return s;
    }
}
