package com.dday.domain.welfare.collector.curation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 제도 → 정규화 카테고리(주거·생활·자산형성·기타). 실제 수집 데이터로 우선순위를 고정한다.
 */
class WelfareCategoryClassifierTest {

    @Test
    void 관심주제에_주거가_있으면_다른_태그보다_먼저_주거() {
        assertThat(WelfareCategoryClassifier.classify("청년월세 지원사업", "주거")).isEqualTo("주거");
        assertThat(WelfareCategoryClassifier.classify("자립정착금", "주거,보호·돌봄,서민금융")).isEqualTo("주거");
    }

    @Test
    void 자산형성은_관심주제가_아니라_제도명으로_판정() {
        // 셋 다 관심주제는 서민금융(곁다리) — 이름의 계좌·공제·적금이 신호
        assertThat(WelfareCategoryClassifier.classify("청년내일저축계좌", "서민금융")).isEqualTo("자산형성");
        assertThat(WelfareCategoryClassifier.classify("청년내일채움공제", "서민금융")).isEqualTo("자산형성");
        assertThat(WelfareCategoryClassifier.classify("청년미래적금", "생활지원,서민금융")).isEqualTo("자산형성");
    }

    @Test
    void 이름에_자산형성_키워드가_없으면_서민금융이어도_생활() {
        // 햇살론youth(저리대출), 학자금대출, 취업지원 — 서민금융 태그지만 적립형이 아니다
        assertThat(WelfareCategoryClassifier.classify("햇살론youth 보증사업", "생활지원,서민금융")).isEqualTo("생활");
        assertThat(WelfareCategoryClassifier.classify("취업 후 상환 학자금대출", "교육,서민금융")).isEqualTo("생활");
        assertThat(WelfareCategoryClassifier.classify("경계선지능청년지원 사업", "서민금융")).isEqualTo("생활");
    }

    @Test
    void 생활계열_태그는_전부_생활로_묶인다() {
        assertThat(WelfareCategoryClassifier.classify("자립수당 지급", "생활지원")).isEqualTo("생활");
        assertThat(WelfareCategoryClassifier.classify("자립지원 전담기관 운영", "보호·돌봄")).isEqualTo("생활");
        assertThat(WelfareCategoryClassifier.classify("고졸 후학습자 장학사업", "교육")).isEqualTo("생활");
        assertThat(WelfareCategoryClassifier.classify("해외취업 지원", "일자리,서민금융")).isEqualTo("생활");
    }

    @Test
    void 관심주제가_없거나_매핑에_없으면_기타() {
        assertThat(WelfareCategoryClassifier.classify("위기청년 전담 지원사업", null)).isEqualTo("기타");
        assertThat(WelfareCategoryClassifier.classify("무언가 지원", "")).isEqualTo("기타");
        assertThat(WelfareCategoryClassifier.classify("출산지원금", "임신·출산")).isEqualTo("기타");
    }

    @Test
    void 지자체_포맷_공백이_섞여도_동일() {
        assertThat(WelfareCategoryClassifier.classify("사회첫걸음수당", "보호·돌봄, 서민금융")).isEqualTo("생활");
    }
}
