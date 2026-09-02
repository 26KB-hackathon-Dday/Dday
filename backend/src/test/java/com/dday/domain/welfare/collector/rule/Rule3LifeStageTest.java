package com.dday.domain.welfare.collector.rule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Rule3LifeStageTest {

    @Test
    void 청년_단독이면_플러스3() {
        assertThat(Rule3LifeStage.apply("청년").delta()).isEqualTo(3);
    }

    @Test
    void 청년에_하나만_더_붙으면_중립() {
        assertThat(Rule3LifeStage.apply("청년,중장년").delta()).isZero();
    }

    @Test
    void 생애주기_셋이면_마이너스2() {
        assertThat(Rule3LifeStage.apply("청년,중장년,노년").delta()).isEqualTo(-2);
    }

    @Test
    void 생애주기_넷이어도_마이너스2() {
        assertThat(Rule3LifeStage.apply("청소년,청년,중장년,노년").delta()).isEqualTo(-2);
    }

    @Test
    void 상세응답처럼_공백이_섞여도_같게_센다() {
        assertThat(Rule3LifeStage.apply("청년, 청소년").delta()).isZero();
        assertThat(Rule3LifeStage.apply(" 청년 ").delta()).isEqualTo(3);
    }

    @Test
    void 값이_없으면_중립() {
        assertThat(Rule3LifeStage.apply(null).delta()).isZero();
        assertThat(Rule3LifeStage.apply("").delta()).isZero();
    }

    @Test
    void 청년이_아닌_단독은_가산점_없음() {
        assertThat(Rule3LifeStage.apply("노년").delta()).isZero();
    }
}
