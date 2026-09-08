package com.dday.domain.welfare.collector.validation;

import com.dday.domain.welfare.entity.SupportAmountType;
import com.dday.domain.welfare.entity.SupportType;
import com.dday.domain.welfare.entity.WelfareProgram;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WelfareProgramValidatorTest {

    private final WelfareProgramValidator validator = new WelfareProgramValidator();

    @Test
    void 다_채워지면_이슈가_없다() {
        WelfareProgram p = program(Map.of(
                "category", "생활",
                "supportType", SupportType.CASH,
                "supportAmount", new BigDecimal("500000"),
                "supportAmountType", SupportAmountType.MONTHLY,
                "targetDescription", "만 19세 ~ 34세 무주택 청년을 지원합니다.",
                "applyChannelName", "보건복지상담센터",
                "applyChannelUrl", "https://www.129.go.kr"));

        assertThat(validator.validate(p)).isEmpty();
        assertThat(validator.needsReview(p)).isFalse();
    }

    @Test
    void 카테고리가_없으면_잡는다() {
        WelfareProgram p = program(Map.of("targetDescription", "청년"));
        assertThat(validator.validate(p)).contains(WelfareIssue.CATEGORY_MISSING);
    }

    @Test
    void 현금성인데_금액이_없으면_잡는다() {
        WelfareProgram p = program(Map.of(
                "category", "생활",
                "supportType", SupportType.CASH,
                "targetDescription", "청년"));
        assertThat(validator.validate(p)).contains(WelfareIssue.CASH_WITHOUT_AMOUNT);
    }

    @Test
    void 비현금은_금액이_없어도_안_잡는다() {
        WelfareProgram p = program(Map.of(
                "category", "자산형성",
                "supportType", SupportType.VOUCHER,
                "targetDescription", "청년"));
        assertThat(validator.validate(p)).doesNotContain(WelfareIssue.CASH_WITHOUT_AMOUNT);
    }

    @Test
    void 월_지원액이_천만원을_넘으면_의심() {
        WelfareProgram p = program(Map.of(
                "category", "생활",
                "supportType", SupportType.CASH,
                "supportAmount", new BigDecimal("48000000"),
                "supportAmountType", SupportAmountType.MONTHLY,
                "targetDescription", "청년"));
        assertThat(validator.validate(p)).contains(WelfareIssue.AMOUNT_SUSPICIOUS);
    }

    @Test
    void 일회성_1천만원은_정상() {
        WelfareProgram p = program(Map.of(
                "category", "생활",
                "supportType", SupportType.CASH,
                "supportAmount", new BigDecimal("10000000"),
                "supportAmountType", SupportAmountType.FIXED,
                "targetDescription", "청년",
                "applyChannelPhone", "129"));
        assertThat(validator.validate(p)).isEmpty();
    }

    @Test
    void 지원대상이_없으면_잡는다() {
        WelfareProgram p = program(Map.of("category", "생활", "applyChannelPhone", "129"));
        assertThat(validator.validate(p)).contains(WelfareIssue.TARGET_MISSING);
    }

    @Test
    void 지원대상이_공고문_형태면_잡는다() {
        // 청년월세 실제 케이스
        WelfareProgram p = program(Map.of(
                "category", "주거",
                "targetDescription", "※ '26년 신규수혜자 신청접수 기간: 3.30(월) 09:00 ~ 5.29(금) 16:00까지",
                "applyChannelPhone", "1599-0001"));
        assertThat(validator.validate(p)).contains(WelfareIssue.TARGET_LOOKS_LIKE_NOTICE);
    }

    @Test
    void 마커_없이_앞머리에_접수기간_단어만_있어도_잡는다() {
        WelfareProgram p = program(Map.of(
                "category", "주거",
                "targetDescription", "신청 기간은 3월 말부터입니다. 무주택 청년 대상.",
                "applyChannelPhone", "1599-0001"));
        assertThat(validator.validate(p)).contains(WelfareIssue.TARGET_LOOKS_LIKE_NOTICE);
    }

    @Test
    void 일반_불릿으로_시작하는_정상_내용은_안_잡는다() {
        // WLF00006199 실제 케이스 — "○ 사업대상 :" 뒤 정상 자격요건
        WelfareProgram p = program(Map.of(
                "category", "생활",
                "targetDescription", "○ 사업대상 : 용산구 거주 자립준비청년 - 아동복지시설, 가정위탁 보호종료 아동",
                "applyChannelPhone", "02-2199-7033"));
        assertThat(validator.validate(p)).doesNotContain(WelfareIssue.TARGET_LOOKS_LIKE_NOTICE);
    }

    @Test
    void 정상_자격요건_서술은_공고문으로_안_본다() {
        WelfareProgram p = program(Map.of(
                "category", "생활",
                "targetDescription", "아래 가입요건을 모두 충족하는 대한민국 거주 청년을 지원합니다. 1. 나이요건 : 만 19세~34세",
                "applyChannelPhone", "1397"));
        assertThat(validator.validate(p)).doesNotContain(WelfareIssue.TARGET_LOOKS_LIKE_NOTICE);
    }

    @Test
    void 신청채널이_전무하면_잡는다() {
        WelfareProgram p = program(Map.of("category", "생활", "targetDescription", "청년"));
        assertThat(validator.validate(p)).contains(WelfareIssue.CHANNEL_MISSING);
    }

    private static WelfareProgram program(Map<String, Object> fields) {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);
        ReflectionTestUtils.setField(p, "servId", "WLF_TEST");
        fields.forEach((k, v) -> ReflectionTestUtils.setField(p, k, v));
        return p;
    }
}
