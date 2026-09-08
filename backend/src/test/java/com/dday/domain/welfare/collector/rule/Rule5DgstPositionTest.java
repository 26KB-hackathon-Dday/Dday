package com.dday.domain.welfare.collector.rule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Rule5DgstPositionTest {

    @Test
    void 청년으로_시작하면_플러스1() {
        assertThat(Rule5DgstPosition.apply("청년에게 자산형성을 지원합니다.").delta()).isEqualTo(1);
    }

    @Test
    void 앞에_공백이_있어도_시작으로_본다() {
        assertThat(Rule5DgstPosition.apply("  청년의 주거 안정을 지원합니다.").delta()).isEqualTo(1);
    }

    @Test
    void 청년이_나열의_첫_항목이면_시작보다_나열이_우선_마이너스1() {
        // 행복주택: "청년, (예비)신혼부부, 한부모가족, 대학생 등..."
        assertThat(Rule5DgstPosition.apply("청년, (예비)신혼부부, 한부모가족, 대학생 등 젊은층의 주거 안정을 위해").delta())
                .isEqualTo(-1);
    }

    @Test
    void 청년이_문장_중간_나열에_끼면_마이너스1() {
        // 햇살론youth: "금융취약계층인 대학생, 청년의 금융애로를..."
        assertThat(Rule5DgstPosition.apply("금융취약계층인 대학생, 청년의 금융애로를 해소하여").delta()).isEqualTo(-1);
        // 국민취업: "저소득 구직자, 청년, 경력단절여성 등..."
        assertThat(Rule5DgstPosition.apply("저소득 구직자, 청년, 경력단절여성 등 취업 취약계층").delta()).isEqualTo(-1);
    }

    @Test
    void 청년층이_주어로_문장_중간에_있으면_중립() {
        // 청년월세: "고금리·고물가 등으로 경제적 어려움을 겪는 청년층의 주거비 부담..."
        assertThat(Rule5DgstPosition.apply("고금리·고물가 등으로 경제적 어려움을 겪는 청년층의 주거비 부담 경감을 위해").delta())
                .isZero();
    }

    @Test
    void 근처에_구분자가_있어도_다른_대상명사가_없으면_중립() {
        // 청년내일저축계좌 2번째 문장: "...예방하고, 일하는 중간계층 청년이 사회에 안착..."
        assertThat(Rule5DgstPosition.apply("근로빈곤층 청년의 하락을 예방하고, 일하는 중간계층 청년이 사회에 안착할 수 있도록").delta())
                .isZero();
    }

    @Test
    void 요약에_청년이_없으면_중립() {
        assertThat(Rule5DgstPosition.apply("대학 학자금 마련에 어려움을 겪는 학생들에게 저리의 대출을 지원합니다.").delta()).isZero();
    }

    @Test
    void 값이_없으면_중립() {
        assertThat(Rule5DgstPosition.apply(null).delta()).isZero();
    }
}
