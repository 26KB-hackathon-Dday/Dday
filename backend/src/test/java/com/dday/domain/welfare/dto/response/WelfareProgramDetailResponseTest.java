package com.dday.domain.welfare.dto.response;

import com.dday.domain.welfare.entity.SupportAmountType;
import com.dday.domain.welfare.entity.SupportType;
import com.dday.domain.welfare.entity.WelfareProgram;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "예상 수입 변화" 카드는 <b>금액 지원형(CASH) + 월별(MONTHLY)</b>일 때만 채워진다.
 * 그 외(1회성·바우처·서비스·대출)에는 {@code incomeChange == null}이라 프론트가 카드를 숨긴다.
 */
class WelfareProgramDetailResponseTest {

    @Test
    void CASH_MONTHLY면_incomeChange가_채워진다() {
        var res = WelfareProgramDetailResponse.from(
                program(SupportType.CASH, SupportAmountType.MONTHLY, new BigDecimal("200000"), 12));

        assertThat(res.getIncomeChange()).isNotNull();
        assertThat(res.getIncomeChange().getMonthlyAmount()).isEqualByComparingTo("200000");
        assertThat(res.getIncomeChange().getDurationMonths()).isEqualTo(12);
        assertThat(res.getIncomeChange().getTitle()).isEqualTo("12개월 간 예상 수입 변화");
    }

    @Test
    void 개월수가_없으면_헤딩이_상시_문구가_된다() {
        var res = WelfareProgramDetailResponse.from(
                program(SupportType.CASH, SupportAmountType.MONTHLY, new BigDecimal("500000"), null));

        assertThat(res.getIncomeChange()).isNotNull();
        assertThat(res.getIncomeChange().getTitle()).isEqualTo("매월 예상 수입 변화");
        assertThat(res.getIncomeChange().getDurationMonths()).isNull();
    }

    @Test
    void 일회성_CASH는_incomeChange가_null() {
        var res = WelfareProgramDetailResponse.from(
                program(SupportType.CASH, SupportAmountType.FIXED, new BigDecimal("10000000"), null));

        assertThat(res.getIncomeChange()).isNull();
    }

    @Test
    void 대출은_월별이어도_incomeChange가_null() {
        var res = WelfareProgramDetailResponse.from(
                program(SupportType.LOAN, SupportAmountType.MONTHLY, new BigDecimal("300000"), 24));

        assertThat(res.getIncomeChange()).isNull();
    }

    @Test
    void 금액이_없으면_incomeChange가_null() {
        var res = WelfareProgramDetailResponse.from(
                program(SupportType.SERVICE, SupportAmountType.MONTHLY, null, null));

        assertThat(res.getIncomeChange()).isNull();
    }

    @Test
    void description은_servDgst를_개행_접어서_담는다() {
        WelfareProgram p = program(SupportType.SERVICE, SupportAmountType.FIXED, null, null);
        ReflectionTestUtils.setField(p, "servDgst", "청년의 자산형성을 지원합니다.\n\n  만기 3년 상품");

        var res = WelfareProgramDetailResponse.from(p);

        assertThat(res.getDescription()).isEqualTo("청년의 자산형성을 지원합니다. 만기 3년 상품");
    }

    @Test
    void servDgst가_없으면_description은_null() {
        var res = WelfareProgramDetailResponse.from(
                program(SupportType.CASH, SupportAmountType.MONTHLY, new BigDecimal("200000"), 12));
        assertThat(res.getDescription()).isNull();
    }

    private static WelfareProgram program(SupportType type, SupportAmountType amountType,
                                          BigDecimal amount, Integer months) {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);
        ReflectionTestUtils.setField(p, "servId", "WLF_TEST");
        ReflectionTestUtils.setField(p, "servNm", "테스트 제도");
        ReflectionTestUtils.setField(p, "supportType", type);
        ReflectionTestUtils.setField(p, "supportAmountType", amountType);
        ReflectionTestUtils.setField(p, "supportAmount", amount);
        ReflectionTestUtils.setField(p, "supportDurationMonths", months);
        return p;
    }
}
