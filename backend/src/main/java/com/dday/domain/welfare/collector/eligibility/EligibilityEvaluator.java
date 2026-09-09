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
 * <p>지역은 시·도 단위로 본다: 제도가 특정 시·도 대상({@code region_code != null})이면 거주
 * 시·도가 같아야 적격이고, 같으면 {@code region}을 근거로 남긴다. 전국·중앙부처 제도
 * ({@code region_code == null})는 지역을 보지 않는다. 시·군·구 정밀도와 나이·소득은 아직 못 본다
 * ({@code User}에 생년월일이 없고, 소득 기준은 {@code eligibility_criteria}가 필요하다).
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

        // 지역 게이트: 특정 시·도 대상 제도(region_code != null)면 거주 시·도가 같아야 한다.
        // 유저 region_code 형식이 환경마다 달라(시드는 법정동 코드, 온보딩은 "시도-시군구" 문자열)
        // 코드 대신 region_name을 시도 2자리로 다시 풀어 비교한다.
        String programRegion = program.getRegionCode();
        boolean regionScoped = programRegion != null && !programRegion.isBlank();
        if (regionScoped
                && !programRegion.equals(RegionCodeResolver.resolve(user.getRegionName(), null))) {
            return EligibilityResult.ineligible(matched,
                    "%s 거주자 대상 제도입니다.".formatted(program.getCtpvNm()));
        }

        if (careLeaver) {
            matched.add("protectionEndDate");
            if (phase != null) {
                matched.add("protectionPhase");
            }
        }
        if (regionScoped) {
            matched.add("region");
        }
        return EligibilityResult.eligible(matched);
    }
}
