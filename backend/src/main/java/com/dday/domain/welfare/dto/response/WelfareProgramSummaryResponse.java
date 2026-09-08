package com.dday.domain.welfare.dto.response;

import com.dday.domain.welfare.entity.WelfareProgram;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 전체 지원제도 목록의 카드 한 장 (SUBSIDY-009).
 *
 * <p>{@code benefitText}·{@code periodText}·{@code status}는 엔티티에 없는 파생값이다
 * ({@link WelfareProgramDisplay}).
 */
@Getter
@AllArgsConstructor
@Builder
public class WelfareProgramSummaryResponse {

    /** 외부 API 서비스 ID (= {@code servId}). 상세 조회 키. */
    private String programId;

    private String name;

    /** 정규화 카테고리 (프론트 칩과 매칭). {@code null}일 수 있다. */
    private String category;

    /** 지원 내용 한 줄 (예: {@code 월 200,000원}). */
    private String benefitText;

    /** 기간 한 줄 (예: {@code ~ 2026.12.31 마감}). */
    private String periodText;

    private ProgramStatus status;

    public static WelfareProgramSummaryResponse from(WelfareProgram p) {
        return WelfareProgramSummaryResponse.builder()
                .programId(p.getServId())
                .name(p.getServNm())
                .category(p.getCategory())
                .benefitText(WelfareProgramDisplay.benefitText(p.getSupportAmount(), p.getSupportAmountType()))
                .periodText(WelfareProgramDisplay.periodText(p.getApplicationDeadline(), p.isOngoingApplication()))
                .status(WelfareProgramDisplay.status(p.getApplicationDeadline(), p.isOngoingApplication()))
                .build();
    }
}
