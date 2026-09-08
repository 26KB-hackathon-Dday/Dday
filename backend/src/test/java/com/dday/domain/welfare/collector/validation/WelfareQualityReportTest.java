package com.dday.domain.welfare.collector.validation;

import com.dday.domain.welfare.entity.WelfareProgram;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WelfareQualityReportTest {

    private final WelfareProgramValidator validator = new WelfareProgramValidator();

    @Test
    void 이슈_건수와_servId를_집계한다() {
        WelfareProgram ok = program("WLF_OK", "생활", "만 19세 청년 대상.", "129");
        WelfareProgram noCategory = program("WLF_A", null, "청년 대상.", "129");
        WelfareProgram noTarget = program("WLF_B", "주거", null, "129");

        WelfareQualityReport report = WelfareQualityReport.of(List.of(ok, noCategory, noTarget), validator);

        assertThat(report.total()).isEqualTo(3);
        assertThat(report.okCount()).isEqualTo(1);
        assertThat(report.reviewCount()).isEqualTo(2);
        assertThat(report.servIdsWith(WelfareIssue.CATEGORY_MISSING)).containsExactly("WLF_A");
        assertThat(report.servIdsWith(WelfareIssue.TARGET_MISSING)).containsExactly("WLF_B");
        assertThat(report.format()).contains("전체 3건", "정상 1건", "리뷰 필요 2건");
    }

    @Test
    void 이슈가_없으면_이슈_없음_문구() {
        WelfareProgram ok = program("WLF_OK", "생활", "만 19세 청년 대상.", "129");
        WelfareQualityReport report = WelfareQualityReport.of(List.of(ok), validator);
        assertThat(report.format()).contains("(이슈 없음)");
    }

    private static WelfareProgram program(String servId, String category, String target, String phone) {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);
        ReflectionTestUtils.setField(p, "servId", servId);
        ReflectionTestUtils.setField(p, "category", category);
        ReflectionTestUtils.setField(p, "targetDescription", target);
        ReflectionTestUtils.setField(p, "applyChannelPhone", phone);
        return p;
    }
}
