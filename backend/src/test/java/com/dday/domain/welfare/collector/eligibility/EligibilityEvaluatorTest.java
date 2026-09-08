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
    void 보호종료일이_없으면_부적격() {
        var result = evaluator.evaluate(user(null), program(null));

        assertThat(result.eligible()).isFalse();
        assertThat(result.ineligibleReason()).contains("보호종료일");
        assertThat(result.matchedCriteria()).isEmpty();
    }

    @Test
    void 보호_종료_전_아동_대상_제도는_부적격() {
        var result = evaluator.evaluate(user(LocalDate.of(2024, 1, 1)), program(ProtectionPhase.PRE_TERMINATION));

        assertThat(result.eligible()).isFalse();
        assertThat(result.ineligibleReason()).contains("보호 종료 전 아동");
        assertThat(result.matchedCriteria()).containsExactly("protectionEndDate");
    }

    @Test
    void 자립준비청년이고_POST_TERMINATION이면_적격() {
        var result = evaluator.evaluate(user(LocalDate.of(2024, 1, 1)), program(ProtectionPhase.POST_TERMINATION));

        assertThat(result.eligible()).isTrue();
        assertThat(result.ineligibleReason()).isNull();
        assertThat(result.matchedCriteria()).containsExactly("protectionEndDate", "protectionPhase");
    }

    @Test
    void protectionPhase가_미분류여도_보호종료일만_있으면_적격() {
        var result = evaluator.evaluate(user(LocalDate.of(2024, 1, 1)), program(null));

        assertThat(result.eligible()).isTrue();
        assertThat(result.matchedCriteria()).containsExactly("protectionEndDate");
    }

    private static User user(LocalDate protectionEndDate) {
        User u = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(u, "protectionEndDate", protectionEndDate);
        return u;
    }

    private static WelfareProgram program(ProtectionPhase phase) {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);
        ReflectionTestUtils.setField(p, "servId", "WLF_TEST");
        ReflectionTestUtils.setField(p, "protectionPhase", phase);
        return p;
    }
}
