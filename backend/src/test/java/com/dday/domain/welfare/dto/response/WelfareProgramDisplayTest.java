package com.dday.domain.welfare.dto.response;

import com.dday.domain.welfare.entity.SupportAmountType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 저장된 구조화 필드 → 화면 문구 파생 규칙. 프론트가 이 문구를 그대로 띄우므로 여기서 고정한다.
 */
class WelfareProgramDisplayTest {

    @Test
    void 금액_타입별_문구() {
        long amount = 200_000L;
        assertThat(WelfareProgramDisplay.benefitText(amount, SupportAmountType.MONTHLY)).isEqualTo("월 200,000원");
        assertThat(WelfareProgramDisplay.benefitText(amount, SupportAmountType.FIXED)).isEqualTo("200,000원 (1회)");
        assertThat(WelfareProgramDisplay.benefitText(amount, SupportAmountType.LIMIT)).isEqualTo("최대 200,000원");
        assertThat(WelfareProgramDisplay.benefitText(amount, SupportAmountType.SEMIANNUAL)).isEqualTo("반기 200,000원");
        assertThat(WelfareProgramDisplay.benefitText(amount, null)).isEqualTo("200,000원");
    }

    @Test
    void 금액이_없으면_문구도_null() {
        assertThat(WelfareProgramDisplay.benefitText(null, SupportAmountType.MONTHLY)).isNull();
    }

    @Test
    void 기간_문구() {
        assertThat(WelfareProgramDisplay.periodText(LocalDate.of(2026, 12, 31), false)).isEqualTo("~ 2026.12.31 마감");
        assertThat(WelfareProgramDisplay.periodText(LocalDate.of(2026, 12, 31), true)).isEqualTo("상시 접수");
        assertThat(WelfareProgramDisplay.periodText(null, false)).isEqualTo("상시 접수");
    }

    @Test
    void 상태_파생() {
        assertThat(WelfareProgramDisplay.status(LocalDate.now().plusDays(1), false)).isEqualTo(ProgramStatus.OPEN);
        assertThat(WelfareProgramDisplay.status(LocalDate.now(), false)).isEqualTo(ProgramStatus.OPEN);
        assertThat(WelfareProgramDisplay.status(null, false)).isEqualTo(ProgramStatus.OPEN);
        assertThat(WelfareProgramDisplay.status(LocalDate.now().minusYears(2), false)).isEqualTo(ProgramStatus.CLOSED);
        // 마감일이 지났어도 상시접수면 OPEN
        assertThat(WelfareProgramDisplay.status(LocalDate.now().minusYears(2), true)).isEqualTo(ProgramStatus.OPEN);
    }
}
