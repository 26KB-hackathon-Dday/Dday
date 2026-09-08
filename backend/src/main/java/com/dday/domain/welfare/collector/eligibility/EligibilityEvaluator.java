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
 * LLM 파싱은 후속이라, 지금은 {@link User}가 실제로 가진 필드로 판정할 수 있는 것만 본다:
 * <ul>
 *   <li>자립준비청년 여부 — {@code protectionEndDate} 등록 여부. 수집된 제도가 전부 자립준비청년
 *       대상이라, 이게 통과 못 하면 아무 제도도 자격이 없다.</li>
 *   <li>{@code protectionPhase} 정합 — 제도가 "보호 종료 전 아동"({@link ProtectionPhase#PRE_TERMINATION})
 *       대상이면 보호종료한 유저는 탈락.</li>
 * </ul>
 *
 * <p>나이·소득·지역은 아직 못 본다 — {@code User}에 생년월일이 없고, 소득 기준(중위소득 %)은
 * {@code eligibility_criteria}가 있어야 비교할 수 있으며, 지역은 행정구역 코드↔이름 매핑 테이블이
 * 필요하다. 전부 후속.
 */
@Component
public class EligibilityEvaluator {

    public EligibilityResult evaluate(User user, WelfareProgram program) {
        List<String> matched = new ArrayList<>();

        if (user.getProtectionEndDate() == null) {
            return EligibilityResult.ineligible(matched, "보호종료일이 등록되어 있지 않습니다.");
        }
        matched.add("protectionEndDate");

        ProtectionPhase phase = program.getProtectionPhase();
        if (phase == ProtectionPhase.PRE_TERMINATION) {
            return EligibilityResult.ineligible(matched, "보호 종료 전 아동 대상 제도입니다.");
        }
        if (phase != null) {
            matched.add("protectionPhase");
        }

        return EligibilityResult.eligible(matched);
    }
}
