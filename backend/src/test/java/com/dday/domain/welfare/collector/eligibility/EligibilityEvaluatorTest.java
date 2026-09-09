package com.dday.domain.welfare.collector.eligibility;

import com.dday.domain.user.entity.User;
import com.dday.domain.welfare.entity.ProtectionPhase;
import com.dday.domain.welfare.entity.WelfareProgram;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityEvaluatorTest {

    private final EligibilityEvaluator evaluator = new EligibilityEvaluator();

    @Test
    void 자립준비청년_전용_제도는_보호종료일이_없으면_부적격() {
        var result = evaluator.evaluate(user(null), program(ProtectionPhase.POST_TERMINATION));

        assertThat(result.eligible()).isFalse();
        assertThat(result.ineligibleReason()).contains("보호종료일");
        assertThat(result.matchedCriteria()).isEmpty();
    }

    @Test
    void 보호_종료_전_아동_대상_제도는_보호종료일이_있어도_부적격() {
        var result = evaluator.evaluate(user(LocalDate.of(2024, 1, 1)), program(ProtectionPhase.PRE_TERMINATION));

        assertThat(result.eligible()).isFalse();
        assertThat(result.ineligibleReason()).contains("보호 종료 전 아동");
    }

    @Test
    void 자립준비청년이고_POST_TERMINATION이면_적격() {
        var result = evaluator.evaluate(user(LocalDate.of(2024, 1, 1)), program(ProtectionPhase.POST_TERMINATION));

        assertThat(result.eligible()).isTrue();
        assertThat(result.ineligibleReason()).isNull();
        assertThat(result.matchedCriteria()).containsExactly("protectionEndDate", "protectionPhase");
    }

    @Test
    void 미분류_제도는_보호종료일이_없어도_적격() {
        var result = evaluator.evaluate(user(null), program(null));

        assertThat(result.eligible()).isTrue();
        assertThat(result.matchedCriteria()).isEmpty();
    }

    @Test
    void 미분류_제도에_보호종료일이_있으면_근거로_남긴다() {
        var result = evaluator.evaluate(user(LocalDate.of(2024, 1, 1)), program(null));

        assertThat(result.eligible()).isTrue();
        assertThat(result.matchedCriteria()).containsExactly("protectionEndDate");
    }

    @Test
    void 특정_시도_대상_제도는_거주_시도가_다르면_부적격() {
        WelfareProgram seoulOnly = program(null);
        ReflectionTestUtils.setField(seoulOnly, "regionCode", "11");
        ReflectionTestUtils.setField(seoulOnly, "ctpvNm", "서울특별시");

        var result = evaluator.evaluate(user(null, "경기도"), seoulOnly);

        assertThat(result.eligible()).isFalse();
        assertThat(result.ineligibleReason()).contains("서울특별시");
    }

    @Test
    void 특정_시도_대상_제도는_거주_시도가_같으면_region을_근거로_남긴다() {
        WelfareProgram seoulOnly = program(null);
        ReflectionTestUtils.setField(seoulOnly, "regionCode", "11");
        ReflectionTestUtils.setField(seoulOnly, "ctpvNm", "서울특별시");

        var result = evaluator.evaluate(user(null, "서울특별시"), seoulOnly);

        assertThat(result.eligible()).isTrue();
        assertThat(result.matchedCriteria()).containsExactly("region");
    }

    @Test
    void 거주지_미등록이면_특정_시도_대상_제도는_부적격() {
        WelfareProgram seoulOnly = program(null);
        ReflectionTestUtils.setField(seoulOnly, "regionCode", "11");
        ReflectionTestUtils.setField(seoulOnly, "ctpvNm", "서울특별시");

        var result = evaluator.evaluate(user(null), seoulOnly);

        assertThat(result.eligible()).isFalse();
    }

    private static User user(LocalDate protectionEndDate) {
        return user(protectionEndDate, null);
    }

    private static User user(LocalDate protectionEndDate, String regionName) {
        User u = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(u, "protectionEndDate", protectionEndDate);
        ReflectionTestUtils.setField(u, "regionName", regionName);
        return u;
    }

    private static WelfareProgram program(ProtectionPhase phase) {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);
        ReflectionTestUtils.setField(p, "servId", "WLF_TEST");
        ReflectionTestUtils.setField(p, "protectionPhase", phase);
        return p;
    }
}
