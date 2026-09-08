package com.dday.domain.welfare.collector.eligibility;

import java.util.List;

/**
 * {@link EligibilityEvaluator} 판정 결과 (한 유저 × 한 제도).
 *
 * @param eligible         자격 있음 여부
 * @param matchedCriteria  통과한 조건 키 (예: {@code ["protectionEndDate", "protectionPhase"]})
 * @param ineligibleReason 불일치 사유. {@code eligible == true}면 {@code null}
 */
public record EligibilityResult(boolean eligible, List<String> matchedCriteria, String ineligibleReason) {

    public static EligibilityResult eligible(List<String> matchedCriteria) {
        return new EligibilityResult(true, List.copyOf(matchedCriteria), null);
    }

    public static EligibilityResult ineligible(List<String> matchedCriteria, String reason) {
        return new EligibilityResult(false, List.copyOf(matchedCriteria), reason);
    }
}
