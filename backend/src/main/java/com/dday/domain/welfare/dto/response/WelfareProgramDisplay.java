package com.dday.domain.welfare.dto.response;

import com.dday.domain.welfare.entity.SupportAmountType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 저장된 구조화 필드(금액·마감일)에서 <b>화면 문구</b>를 만든다.
 *
 * <p>프론트가 각자 "월 최대 200,000원" 같은 문구를 조립하면 화면마다 표기가 갈리므로
 * 서버가 한 곳에서 만든다 (PocketResponse가 소진율을 서버에서 계산하는 것과 같은 이유).
 * 목록·상세 응답이 공유한다.
 */
final class WelfareProgramDisplay {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private WelfareProgramDisplay() {
    }

    /** 예: {@code 월 200,000원} / {@code 1,000만원은 표기 안 함 — 원 단위로 그대로}. */
    static String benefitText(BigDecimal amount, SupportAmountType type) {
        if (amount == null) {
            return null;
        }
        String won = String.format("%,d원", amount.setScale(0, RoundingMode.HALF_UP).longValueExact());
        if (type == null) {
            return won;
        }
        return switch (type) {
            case MONTHLY -> "월 " + won;
            case FIXED -> won + " (1회)";
            case LIMIT -> "최대 " + won;
            case SEMIANNUAL -> "반기 " + won;
        };
    }

    /** 예: {@code ~ 2026.12.31 마감} / {@code 상시 접수}. */
    static String periodText(LocalDate deadline, boolean ongoing) {
        if (ongoing || deadline == null) {
            return "상시 접수";
        }
        return "~ " + deadline.format(YMD) + " 마감";
    }

    /** 상시접수이거나 마감일이 없거나 오늘 이후면 OPEN. */
    static ProgramStatus status(LocalDate deadline, boolean ongoing) {
        if (ongoing || deadline == null || !deadline.isBefore(LocalDate.now())) {
            return ProgramStatus.OPEN;
        }
        return ProgramStatus.CLOSED;
    }

    /** 예: {@code 최대 12개월간 지원}. 개월 수가 없으면 {@code null}. */
    static String durationText(Integer months) {
        return months == null ? null : "최대 " + months + "개월간 지원";
    }

    /** "예상 수입 변화" 카드 헤딩. 예: {@code 12개월 간 예상 수입 변화} / {@code 매월 예상 수입 변화}. */
    static String incomeChangeTitle(Integer months) {
        return months == null ? "매월 예상 수입 변화" : months + "개월 간 예상 수입 변화";
    }
}
