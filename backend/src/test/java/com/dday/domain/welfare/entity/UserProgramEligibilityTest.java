package com.dday.domain.welfare.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserProgramEligibilityTest {

    private final UUID userId = UUID.randomUUID();

    @Test
    void 생성시_복합키와_기본값이_잡힌다() {
        var e = new UserProgramEligibility(userId, "WLF00001175");

        assertThat(e.getId().getUserId()).isEqualTo(userId);
        assertThat(e.getId().getProgramId()).isEqualTo("WLF00001175");
        assertThat(e.isEligible()).isFalse();
        assertThat(e.getEvaluatedAt()).isNotNull();
    }

    @Test
    void 자격이면_불일치사유를_비우고_충족조건을_담는다() {
        var e = new UserProgramEligibility(userId, "WLF00001175");

        e.applyEvaluation(true, "[\"protectionEndDate\",\"region\"]", "소득 기준 초과");

        assertThat(e.isEligible()).isTrue();
        assertThat(e.getMatchedCriteria()).isEqualTo("[\"protectionEndDate\",\"region\"]");
        assertThat(e.getIneligibleReason()).isNull();
    }

    @Test
    void 부적격이면_사유를_남긴다() {
        var e = new UserProgramEligibility(userId, "WLF00001175");

        e.applyEvaluation(false, null, "소득 기준 초과");

        assertThat(e.isEligible()).isFalse();
        assertThat(e.getIneligibleReason()).isEqualTo("소득 기준 초과");
    }

    @Test
    void 재판별하면_evaluatedAt이_갱신된다() throws InterruptedException {
        var e = new UserProgramEligibility(userId, "WLF00001175");
        var first = e.getEvaluatedAt();
        Thread.sleep(2);

        e.applyEvaluation(true, null, null);

        assertThat(e.getEvaluatedAt()).isAfter(first);
    }
}
