package com.dday.domain.welfare.dto.response;

import com.dday.domain.welfare.entity.AgencyType;
import com.dday.domain.welfare.entity.SupportAmountType;
import com.dday.domain.welfare.entity.SupportType;
import com.dday.domain.welfare.entity.WelfareProgram;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 리뷰 큐 한 줄 (관리자용, {@code /internal/welfare/review-queue}).
 *
 * <p>관리자가 "무엇을 왜 고쳐야 하나"를 한눈에 보도록 걸린 사유({@code issues})와 문제 필드의
 * <b>현재 값</b>을 같이 준다. 원문은 {@code raw_list_xml}/{@code raw_detail_xml} 컬럼에 있고
 * ({@code hasRawDetailXml}로 존재 여부만 표시), 복지로 원본은 {@code detailLink}.
 */
@Getter
@AllArgsConstructor
@Builder
public class WelfareReviewItemResponse {

    private String programId;
    private String name;
    private AgencyType agencyType;

    /** 걸린 점검 항목 이름 (예: {@code [CASH_WITHOUT_AMOUNT, TARGET_LOOKS_LIKE_NOTICE]}). */
    private List<String> issues;

    // ── 문제일 수 있는 현재 값 ──
    private String category;
    private SupportType supportType;
    private Long supportAmount;
    private SupportAmountType supportAmountType;
    private Integer supportDurationMonths;
    private String targetDescription;
    private String applyChannelName;
    private String applyChannelUrl;
    private String applyChannelPhone;

    /** 복지로 원본 상세 링크 (목록 API {@code servDtlLink}). */
    private String detailLink;
    private boolean hasRawListXml;
    private boolean hasRawDetailXml;

    private LocalDateTime collectedAt;
    private LocalDateTime updatedAt;

    public static WelfareReviewItemResponse from(WelfareProgram p) {
        return WelfareReviewItemResponse.builder()
                .programId(p.getServId())
                .name(p.getServNm())
                .agencyType(p.getAgencyType())
                .issues(splitIssues(p.getCurationIssues()))
                .category(p.getCategory())
                .supportType(p.getSupportType())
                .supportAmount(p.getSupportAmount())
                .supportAmountType(p.getSupportAmountType())
                .supportDurationMonths(p.getSupportDurationMonths())
                .targetDescription(p.getTargetDescription())
                .applyChannelName(p.getApplyChannelName())
                .applyChannelUrl(p.getApplyChannelUrl())
                .applyChannelPhone(p.getApplyChannelPhone())
                .detailLink(p.getDetailLink())
                .hasRawListXml(p.getRawListXml() != null)
                .hasRawDetailXml(p.getRawDetailXml() != null)
                .collectedAt(p.getCollectedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private static List<String> splitIssues(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
