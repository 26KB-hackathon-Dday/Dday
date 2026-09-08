package com.dday.domain.welfare.dto.response;

import com.dday.domain.welfare.collector.curation.DetailText;
import com.dday.domain.welfare.entity.SupportAmountType;
import com.dday.domain.welfare.entity.SupportType;
import com.dday.domain.welfare.entity.WelfareProgram;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 제도별 신청 안내 상세 (SUBSIDY-005).
 *
 * <p>구조화 필드(금액·마감일)와 파생 문구를 함께 내려, 프론트가 그대로 렌더하거나
 * 필요하면 재가공할 수 있게 한다.
 */
@Getter
@AllArgsConstructor
@Builder
public class WelfareProgramDetailResponse {

    private String programId;
    private String name;

    /** 운영 기관. CENTRAL이면 소관 부처, LOCAL이면 담당 부서 문자열. */
    private String agency;

    private String category;

    /** 제도 한 줄 설명 (목록 API {@code servDgst}). 목적·취지를 담은 요약문. */
    private String description;

    private String targetDescription;

    private BigDecimal supportAmount;
    private SupportAmountType supportAmountType;
    private Integer supportDurationMonths;
    private LocalDate applicationDeadline;
    private boolean ongoingApplication;

    /** 파생 — {@link WelfareProgramDisplay} */
    private String benefitText;
    /** 지원 기간 문구 (예: {@code 최대 12개월간 지원}). 개월 수 없으면 {@code null}. */
    private String benefitNote;
    private String periodText;
    private ProgramStatus status;

    /**
     * "예상 수입 변화" 카드용 데이터. <b>금액 지원형(CASH)이고 월별(MONTHLY) 지급일 때만</b>
     * 채워지고, 그 외(1회성·바우처·서비스·대출)에는 {@code null}이다.
     * 프론트는 {@code null} 여부로 카드 노출을 가른다.
     *
     * <p>"현재 / 수령 시" 절대 금액(유저 월소득 기준)은 여기 없다 — 그건 마이데이터가 필요해
     * SUBSIDY-006(포켓 반영 시뮬레이션)에서 채운다. 여기서는 제도 사실(월 지원액·개월 수)만.
     */
    private IncomeChange incomeChange;

    private List<String> requiredDocuments;

    private ApplicationChannel applicationChannel;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ApplicationChannel {
        private String name;
        private String url;
        private String phone;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class IncomeChange {
        /** 카드 헤딩 (예: {@code 12개월 간 예상 수입 변화}). */
        private String title;
        /** 월 지원 금액 (원). */
        private BigDecimal monthlyAmount;
        /** 지원 개월 수. {@code null}이면 기간 미정/상시. */
        private Integer durationMonths;
    }

    public static WelfareProgramDetailResponse from(WelfareProgram p) {
        return WelfareProgramDetailResponse.builder()
                .programId(p.getServId())
                .name(p.getServNm())
                .agency(p.getJurMnofNm() != null ? p.getJurMnofNm() : p.getBizChrDeptNm())
                .category(p.getCategory())
                .description(normalizeWhitespace(p.getServDgst()))
                .targetDescription(p.getTargetDescription())
                .supportAmount(p.getSupportAmount())
                .supportAmountType(p.getSupportAmountType())
                .supportDurationMonths(p.getSupportDurationMonths())
                .applicationDeadline(p.getApplicationDeadline())
                .ongoingApplication(p.isOngoingApplication())
                .benefitText(WelfareProgramDisplay.benefitText(p.getSupportAmount(), p.getSupportAmountType()))
                .benefitNote(WelfareProgramDisplay.durationText(p.getSupportDurationMonths()))
                .periodText(WelfareProgramDisplay.periodText(p.getApplicationDeadline(), p.isOngoingApplication()))
                .status(WelfareProgramDisplay.status(p.getApplicationDeadline(), p.isOngoingApplication()))
                .incomeChange(incomeChangeOf(p))
                .requiredDocuments(splitDocs(p.getRequiredDocuments()))
                .applicationChannel(ApplicationChannel.builder()
                        .name(p.getApplyChannelName())
                        // 기존 적재분에 스킴 없는 URL이 남아 있어 응답 시점에도 한 번 더 정리한다.
                        .url(DetailText.normalizeUrl(p.getApplyChannelUrl()))
                        .phone(p.getApplyChannelPhone())
                        .build())
                .build();
    }

    /** 금액 지원형(CASH) + 월별(MONTHLY) + 금액이 있을 때만. 그 외엔 {@code null}. */
    private static IncomeChange incomeChangeOf(WelfareProgram p) {
        boolean recurringCash = p.getSupportType() == SupportType.CASH
                && p.getSupportAmountType() == SupportAmountType.MONTHLY
                && p.getSupportAmount() != null;
        if (!recurringCash) {
            return null;
        }
        return IncomeChange.builder()
                .title(WelfareProgramDisplay.incomeChangeTitle(p.getSupportDurationMonths()))
                .monthlyAmount(p.getSupportAmount())
                .durationMonths(p.getSupportDurationMonths())
                .build();
    }

    /** {@code servDgst}에 개행·중복 공백이 섞여 있어 한 줄 문구로 접는다. */
    private static String normalizeWhitespace(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.replaceAll("\\s+", " ").strip();
    }

    private static List<String> splitDocs(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
