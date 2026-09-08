package com.dday.domain.welfare.collector.curation;

import com.dday.domain.welfare.entity.SupportAmountType;
import org.junit.jupiter.api.Test;

import static com.dday.domain.welfare.entity.SupportAmountType.FIXED;
import static com.dday.domain.welfare.entity.SupportAmountType.MONTHLY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상세 {@code alwServCn} 자연어 → 금액·개월. 실제 응답 문장으로 고정한다.
 */
class SupportAmountParserTest {

    @Test
    void 매월_만원() {
        var p = SupportAmountParser.parse("자립수당 결정 대상자 명의 계좌로 매월 50만원을 지급합니다.", MONTHLY);
        assertThat(p.amount()).isEqualTo(500_000L);
        assertThat(p.months()).isNull();
    }

    @Test
    void 월_원_콤마_그리고_개월() {
        var p = SupportAmountParser.parse("· 월 300,000원, 최대 60개월 지급", MONTHLY);
        assertThat(p.amount()).isEqualTo(300_000L);
        assertThat(p.months()).isEqualTo(60);
    }

    @Test
    void 월별이면_총액이_아니라_월_금액을_집는다() {
        // 청년월세 실제 문장 — "최대 480만원(월 최대 20만원)"
        var p = SupportAmountParser.parse(
                "실제 납부하는 임대료를 최대 480만원(월 최대 20만원)까지 최대 24개월(회) 동안 매월 지원합니다.", MONTHLY);
        assertThat(p.amount()).isEqualTo(200_000L); // 480만원 아님
        assertThat(p.months()).isEqualTo(24);
    }

    @Test
    void 시간당_표기만_있으면_금액_없음() {
        // 근로장학금 실제 문장
        var p = SupportAmountParser.parse("근로시간에 따라 매월 지급합니다. (교내근로) 시간당 10,320원", MONTHLY);
        assertThat(p.amount()).isNull();
    }

    @Test
    void 일회성은_그대로_첫_금액() {
        var p = SupportAmountParser.parse("보호종료 시 자립정착금 1,000만원을 1회 지급합니다.", FIXED);
        assertThat(p.amount()).isEqualTo(10_000_000L);
    }

    @Test
    void 년만_있으면_12배() {
        var p = SupportAmountParser.parse("매월 20만원씩 3년 동안", MONTHLY);
        assertThat(p.amount()).isEqualTo(200_000L);
        assertThat(p.months()).isEqualTo(36);
    }

    @Test
    void 금액이_없으면_null() {
        var p = SupportAmountParser.parse("소득·재산 조사 후 결정된 금액을 지급합니다.", null);
        assertThat(p.amount()).isNull();
        assertThat(p.months()).isNull();
    }

    @Test
    void 빈_입력() {
        assertThat(SupportAmountParser.parse(null, MONTHLY).amount()).isNull();
        assertThat(SupportAmountParser.parse("", (SupportAmountType) null).months()).isNull();
    }
}
