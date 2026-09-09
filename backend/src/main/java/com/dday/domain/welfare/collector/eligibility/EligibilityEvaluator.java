package com.dday.domain.welfare.collector.eligibility;

import com.dday.domain.user.entity.User;
import com.dday.domain.welfare.collector.curation.RegionCodeResolver;
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
 * <p>지역은 법정동 코드 접두 비교로 본다: 지역 한정 제도({@code region_code != null})는 거주지가
 * 그 코드 범위 안에 들어야 적격이고({@code userCode.startsWith(programCode)}), 들면 {@code region}을
 * 근거로 남긴다. 제도는 시·도(2자리) 또는 등록된 시·군·구(10자리, {@code RegionCodeResolver.SIGUNGU})
 * 단위다. 전국·중앙부처 제도({@code region_code == null})는 지역을 보지 않는다. 나이·소득은 아직
 * 못 본다({@code User}에 생년월일이 없고, 소득 기준은 {@code eligibility_criteria}가 필요하다).
 * 그래서 홈은 이걸 곧장 "받는 지원"이 아니라 "확인 필요"로 흘려보내 사용자에게 되묻는다.
 * 정밀 판정은 후속.
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

        // 지역 게이트: 지역 한정 제도(region_code != null)는 거주지가 그 안에 들어야 한다.
        // 제도 코드가 유저 법정동 코드의 접두면 포함으로 본다:
        //   제도 "11"(서울) ⊃ 유저 "1117000000"(용산) → 매칭
        //   제도 "1141000000"(서대문) vs 유저 "1117000000"(용산) → 불일치
        // 유저 region_code는 형식이 들쭉날쭉해(시드=코드, 온보딩=문자열) 신뢰하지 않고,
        // region_name+district_name을 RegionCodeResolver로 다시 푼다.
        String programRegion = program.getRegionCode();
        if (programRegion != null && !programRegion.isBlank()) {
            String userRegion = RegionCodeResolver.resolve(user.getRegionName(), user.getDistrictName());
            if (userRegion == null || !userRegion.startsWith(programRegion)) {
                return EligibilityResult.ineligible(matched,
                        "%s 거주자 대상 제도입니다.".formatted(program.getCtpvNm()));
            }
            matched.add("region");
        }

        return EligibilityResult.eligible(matched);
    }
}
