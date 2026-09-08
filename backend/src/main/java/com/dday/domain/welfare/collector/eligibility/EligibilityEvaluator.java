package com.dday.domain.welfare.collector.eligibility;

import com.dday.domain.user.entity.User;
import com.dday.domain.welfare.entity.ProtectionPhase;
import com.dday.domain.welfare.entity.WelfareProgram;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 유저 프로필 ↔ 제도 자격 조건 대조 (SUBSIDY-002).
 *
 * <p><b>v1 — 결정론, LLM 없이.</b> {@code welfare_program.eligibility_criteria}(구조화 자격 조건)와
 * LLM 파싱은 후속이라, 지금은 {@code protectionPhase} 하나로만 가른다:
 * <ul>
 *   <li>{@link ProtectionPhase#PRE_TERMINATION}(보호 종료 전 아동 대상) → 항상 부적격</li>
 *   <li>{@link ProtectionPhase#POST_TERMINATION}(자립준비청년 전용) → {@code protectionEndDate}가
 *       등록돼 있어야 적격. 없으면 "보호종료일을 등록하면 매칭됩니다"로 안내</li>
 *   <li>{@code null}(일반 대상)·{@link ProtectionPhase#BOTH} → 자립준비청년 여부와 무관하게 적격.
 *       청년월세·청년적금처럼 자립준비청년이 아니어도 받는 제도가 여기 해당한다</li>
 * </ul>
 *
 * <p>일반 제도를 "적격"으로 내보내는 건 낙관적이다 — 나이·소득·지역을 아직 못 본다
 * ({@code User}에 생년월일이 없고, 소득 기준은 {@code eligibility_criteria}가, 지역은 코드↔이름
 * 매핑 테이블이 필요하다). 그래서 홈은 이걸 곧장 "받는 지원"이 아니라 "확인 필요"로 흘려보내
 * 사용자에게 되묻는다. 정밀 판정은 후속.
 */
@Component
public class EligibilityEvaluator {

    public EligibilityResult evaluate(User user, WelfareProgram program) {
        List<String> matched = new ArrayList<>();
        ProtectionPhase phase = program.getProtectionPhase();
        boolean careLeaver = user.getProtectionEndDate() != null;

        if (phase == ProtectionPhase.PRE_TERMINATION) {
            return EligibilityResult.ineligible(matched, "보호 종료 전 아동 대상 제도입니다.");
        }
        if (phase == ProtectionPhase.POST_TERMINATION && !careLeaver) {
            return EligibilityResult.ineligible(matched,
                    "자립준비청년(보호종료) 대상 제도입니다. 보호종료일을 등록하면 매칭됩니다.");
        }

        if (careLeaver) {
            matched.add("protectionEndDate");
            if (phase != null) {
                matched.add("protectionPhase");
            }
        }
        return EligibilityResult.eligible(matched);
    }
}
