package com.dday.domain.welfare.collector.curation;

import com.dday.domain.welfare.entity.SupportAmountType;

/**
 * 수집한 {@code sprtCycNm}(지원주기 원본: {@code 월}·{@code 1회성}·{@code 반기}·{@code 수시})를
 * {@link SupportAmountType}으로 옮긴다.
 *
 * <p>금액 <b>타입</b>은 목록 API로 이렇게 채울 수 있지만, 금액 <b>숫자</b>는 목록 API에 없어
 * (급여내용 {@code alwServCn}은 상세 API의 자연어 필드) 상세보강 단계에서 채운다.
 */
public final class SupportCycleMapper {

    private SupportCycleMapper() {
    }

    /** 매핑이 안 되는 주기({@code 수시}, {@code null} 등)는 {@code null} — 상세보강이 재판정. */
    public static SupportAmountType toAmountType(String sprtCycNm) {
        if (sprtCycNm == null) {
            return null;
        }
        return switch (sprtCycNm.trim()) {
            case "월" -> SupportAmountType.MONTHLY;
            case "1회성" -> SupportAmountType.FIXED;
            case "반기" -> SupportAmountType.SEMIANNUAL;
            default -> null; // 수시, 연, 분기 등
        };
    }
}
