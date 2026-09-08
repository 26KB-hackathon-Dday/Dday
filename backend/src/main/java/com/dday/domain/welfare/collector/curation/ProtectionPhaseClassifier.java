package com.dday.domain.welfare.collector.curation;

import com.dday.domain.welfare.entity.ProtectionPhase;

import java.util.regex.Pattern;

/**
 * 제도가 <b>자립준비청년(보호종료 후) 전용</b>인지, 아직 <b>보호 중인 아동</b> 대상인지 태깅한다.
 * 결과는 {@code welfare_program.protection_phase}에 들어가고, 자격 판별
 * ({@code EligibilityEvaluator})이 읽는다.
 *
 * <p><b>키워드 규칙 — LLM 분류로 가는 다리다.</b> 신청자격 원문(지원대상)이나 제도명에
 * "자립준비청년" 또는 "보호종료"가 들어가면 자립준비청년 전용으로 본다. 수집셋(2026-09) 18건에서
 * 이 규칙은 자립 6건을 정확히 6건 다 잡고 오탐이 없다.
 *
 * <p>한계: "보호종료 아동 <b>우대</b>"처럼 요건이 아니라 가점으로만 언급하는 제도가 새로 들어오면
 * 오탐이 난다. 아직 자동 감지가 없어, 그때는 관리자가 {@code protection_phase}를 직접 고쳐야 한다
 * ({@link com.dday.domain.welfare.entity.ProgramSource#MANUAL_CURATION}).
 *
 * <p>제대로 된 버전은 {@code slctCritCn}(신청자격 원문)을 LLM으로 나이·소득·지역·자립여부
 * 구조화 조건으로 파싱하는 것 — 그때 이 클래스는 사라진다.
 */
public final class ProtectionPhaseClassifier {

    /** 보호종료 후 = 자립준비청년 전용임을 드러내는 표현. */
    private static final Pattern CARE_LEAVER = Pattern.compile(
            "자립준비청년|보호\\s*(가\\s*)?(조기\\s*)?종[료결]");

    /** 아직 보호 중인 아동 대상임을 드러내는 표현. 종료·자립 언급이 전혀 없을 때만 본다. */
    private static final Pattern IN_CARE = Pattern.compile("보호대상아동|보호\\s*중인?\\s*아동");

    private ProtectionPhaseClassifier() {
    }

    /**
     * @return {@link ProtectionPhase#POST_TERMINATION} / {@link ProtectionPhase#PRE_TERMINATION},
     *         둘 다 아니면 {@code null}(일반 대상 / 미분류)
     */
    public static ProtectionPhase classify(String servNm, String servDgst, String targetDescription) {
        String text = join(servNm, servDgst, targetDescription);
        if (text.isBlank()) {
            return null;
        }
        if (CARE_LEAVER.matcher(text).find()) {
            return ProtectionPhase.POST_TERMINATION;
        }
        if (IN_CARE.matcher(text).find()) {
            return ProtectionPhase.PRE_TERMINATION;
        }
        return null;
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null) {
                sb.append(part).append(' ');
            }
        }
        return sb.toString();
    }
}
