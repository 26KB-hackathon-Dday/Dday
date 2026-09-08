package com.dday.domain.welfare.collector.curation;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 수집한 제도를 프론트 칩과 같은 <b>정규화 카테고리 한 개</b>로 분류한다.
 * 결과는 {@code 주거} · {@code 생활} · {@code 자산형성} · {@code 기타} 넷 중 하나.
 *
 * <p>판정 순서(우선순위):
 * <ol>
 *   <li>관심주제에 {@code 주거}가 있으면 → 주거</li>
 *   <li>제도명에 적금·계좌·공제·저축·통장이 있으면 → 자산형성
 *       (관심주제 {@code 서민금융}은 곁다리로 너무 자주 붙어 신호가 안 된다. 자산형성 제도의
 *       진짜 신호는 이름이다 — 청년내일저축<b>계좌</b>, 청년내일채움<b>공제</b>, 청년미래<b>적금</b>)</li>
 *   <li>관심주제에 생활계열이나 {@code 서민금융}이 있으면 → 생활 (자립준비청년 대상 제도는
 *       대부분 "생활 유지" 범주: 수당·학자금·취업)</li>
 *   <li>그 외(관심주제가 없거나 매핑에 없는 것만) → 기타</li>
 * </ol>
 *
 * <p>금액 숫자({@code supportAmount})는 목록 API에 없어 여기서 못 채운다 — 상세보강(후속).
 */
public final class WelfareCategoryClassifier {

    public static final String HOUSING = "주거";
    public static final String LIVING = "생활";
    public static final String ASSET = "자산형성";
    public static final String ETC = "기타";

    /** 자산형성(적립형) 제도임을 드러내는 제도명 키워드. */
    private static final Pattern ASSET_NAME = Pattern.compile("적금|계좌|공제|저축|통장");

    /** "생활 유지"로 묶는 관심주제 토큰. */
    private static final Set<String> LIVING_THEMES = Set.of(
            "생활지원", "교육", "일자리", "보호·돌봄", "입양·위탁", "안전·위기", "정신건강", "서민금융");

    private WelfareCategoryClassifier() {
    }

    /**
     * @param servNm          제도명 (예: {@code 청년내일저축계좌})
     * @param intrsThemaArray {@code "생활지원,서민금융"} 형태. 지자체 응답도 {@code toCommon()}에서
     *                        공백이 제거돼 중앙과 같은 포맷. {@code null}/빈 값 허용.
     */
    public static String classify(String servNm, String intrsThemaArray) {
        List<String> themes = parseThemes(intrsThemaArray);

        if (themes.contains(HOUSING)) {
            return HOUSING;
        }
        if (servNm != null && ASSET_NAME.matcher(servNm).find()) {
            return ASSET;
        }
        if (themes.stream().anyMatch(LIVING_THEMES::contains)) {
            return LIVING;
        }
        return ETC;
    }

    private static List<String> parseThemes(String intrsThemaArray) {
        if (intrsThemaArray == null || intrsThemaArray.isBlank()) {
            return List.of();
        }
        return List.of(intrsThemaArray.split("\\s*,\\s*"));
    }
}
