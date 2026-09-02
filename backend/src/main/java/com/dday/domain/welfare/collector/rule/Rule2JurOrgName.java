package com.dday.domain.welfare.collector.rule;

/**
 * Rule 2 — 소관 부서명 신호. {@code jurOrgNm}에 "청년"이 들어가면 +2.
 *
 * <p>"청년주거정책과", "청년장학지원과"처럼 부서 이름에 청년이 박혀 있으면 그 부서가 청년
 * 사업을 전담한다는 뜻이라 신뢰도가 높다 — docs/welfare-api/NOTES.md §7-2.
 *
 * <p>상세보강 단계에서는 {@code jurOrgNm} 분리 필드가 없어 {@code jurMnofNm}(부처+팀 합쳐짐)을
 * 대신 넘긴다.
 */
public final class Rule2JurOrgName {

    private Rule2JurOrgName() {
    }

    public static RuleHit apply(String jurOrgName) {
        if (jurOrgName != null && jurOrgName.contains("청년")) {
            return RuleHit.of("R2", 2);
        }
        return RuleHit.neutral();
    }
}
