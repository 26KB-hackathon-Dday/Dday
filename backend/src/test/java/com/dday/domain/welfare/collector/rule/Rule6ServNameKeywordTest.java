package com.dday.domain.welfare.collector.rule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Rule6ServNameKeywordTest {

    @Test
    void 서비스명에_청년이_있으면_플러스2() {
        assertThat(Rule6ServNameKeyword.apply("청년창업농장학금 지원").delta()).isEqualTo(2);
        assertThat(Rule6ServNameKeyword.apply("서민금융 활성화 지원(청년 보증사업)").delta()).isEqualTo(2);
    }

    @Test
    void 청년이_없으면_중립() {
        assertThat(Rule6ServNameKeyword.apply("취업 후 상환 학자금대출").delta()).isZero();
        // "youth"는 "청년"이 아니다
        assertThat(Rule6ServNameKeyword.apply("서민금융 활성화 지원(햇살론youth 보증사업)").delta()).isZero();
    }

    @Test
    void null이면_중립() {
        assertThat(Rule6ServNameKeyword.apply(null).delta()).isZero();
    }
}
