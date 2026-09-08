package com.dday.domain.welfare.dto.response;

import com.dday.domain.welfare.entity.UserProgramEligibility;
import com.dday.domain.welfare.entity.WelfareProgram;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 지원금 매칭 홈 대시보드 (SUBSIDY-002/003/004 통합).
 *
 * <p>"확인된 지원" = 자격 있는 제도 전부. 그 안에서 수급 상태로 세 갈래:
 * <ul>
 *   <li>{@code receivingList} — 받고 있다({@code RECEIVING})</li>
 *   <li>{@code missing} — 안 받는다({@code NOT_RECEIVING}). 놓치고 있을 수 있는 지원</li>
 *   <li>{@code reviewQueue} — 아직 확인 안 함({@code UNKNOWN}/status 행 없음) 또는 자동탐지 제안({@code LIKELY_RECEIVING})</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
@Builder
public class SubsidyHomeResponse {

    private Summary summary;
    private List<SubsidyCard> receivingList;
    private List<SubsidyCard> missing;
    private List<SubsidyCard> reviewQueue;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Summary {
        /** 확인된 지원 = 자격 있는 제도 총계. */
        private int confirmed;
        private int receiving;
        /** 놓치고 있을 수 있는 지원 수. */
        private int actionNeeded;
        /** 확인 필요한 지원 수. */
        private int needsReview;
        /** 놓치고 있는 제도의 월 환산 지원액 합계 (원). */
        private long totalMonthlyEquivalentAmount;
        /** 마지막 자격 재판별 시각. */
        private LocalDateTime evaluatedAt;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class SubsidyCard {
        private String programId;
        private String name;
        private String category;
        /** 예: {@code 월 200,000원}. 금액 미상이면 {@code null}. */
        private String benefitText;
        /** 월 환산 지원액 (원). {@code FIXED}는 개월 수로 나눔. 미상이면 {@code null}. */
        private Long monthlyEquivalentAmount;
        /** 자격 판별에서 통과한 조건 키 (예: {@code ["protectionEndDate"]}). "왜 추천됐나요" 근거. */
        private List<String> matchedCriteria;

        public static SubsidyCard of(WelfareProgram p, UserProgramEligibility e, Long monthlyEquivalent,
                                     List<String> matchedCriteria) {
            return SubsidyCard.builder()
                    .programId(p.getServId())
                    .name(p.getServNm())
                    .category(p.getCategory())
                    .benefitText(WelfareProgramDisplay.benefitText(p.getSupportAmount(), p.getSupportAmountType()))
                    .monthlyEquivalentAmount(monthlyEquivalent)
                    .matchedCriteria(matchedCriteria)
                    .build();
        }
    }
}
