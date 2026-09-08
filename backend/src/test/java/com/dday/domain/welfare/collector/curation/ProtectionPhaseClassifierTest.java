package com.dday.domain.welfare.collector.curation;

import com.dday.domain.welfare.entity.ProtectionPhase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 지원대상·제도명 → 보호단계. 실제 수집셋(2026-09) 18건 기준으로 고정한다.
 * 자립준비청년 6건은 전부 POST_TERMINATION, 나머지 12건은 전부 null이어야 한다.
 */
class ProtectionPhaseClassifierTest {

    @Test
    void 지원대상에_자립준비청년이_있으면_POST_TERMINATION() {
        assertThat(ProtectionPhaseClassifier.classify(
                "자립준비청년 자립수당 지급", null,
                "아동복지시설, 가정위탁 보호종료 5년 이내 자립준비청년(보호종료아동)을 지원합니다."))
                .isEqualTo(ProtectionPhase.POST_TERMINATION);
    }

    @Test
    void 제도명에만_자립준비청년이_있어도_POST_TERMINATION() {
        assertThat(ProtectionPhaseClassifier.classify(
                "서대문구 자립준비청년 자립지원(사회첫걸음)수당", null, null))
                .isEqualTo(ProtectionPhase.POST_TERMINATION);
    }

    @Test
    void 자립준비청년_단어가_없어도_보호종료_표현이면_POST_TERMINATION() {
        assertThat(ProtectionPhaseClassifier.classify(
                "자립정착금", "보호 종결 후 자립하여 생활할 수 있도록 정착금을 지원",
                "15세 이후 아동복지시설·가정위탁 보호가 조기 종료되었거나 18세 이후 보호종료된 자"))
                .isEqualTo(ProtectionPhase.POST_TERMINATION);
    }

    @Test
    void 일반_청년_제도는_null() {
        assertThat(ProtectionPhaseClassifier.classify(
                "청년월세 지원사업", "청년층의 주거비 부담 경감을 위해 월세를 지원합니다",
                "19세~34세 독립거주 무주택 청년 중 청년가구 소득이 기준 중위소득 60% 이하"))
                .isNull();
        assertThat(ProtectionPhaseClassifier.classify(
                "청년내일저축계좌", "근로빈곤층 청년의 자산형성을 지원합니다",
                "가입연령 : 신청 당시 만 15세~ 39세"))
                .isNull();
    }

    @Test
    void 자립이라는_단어가_다른_맥락으로_있어도_오탐하지_않는다() {
        // "노동을 통한 자립을 지원" — 자립준비청년이 아니다
        assertThat(ProtectionPhaseClassifier.classify(
                "경계선지능청년지원 사업", "노동을 통한 자립을 지원합니다.",
                "경계선 지능(IQ 71~84) 청년(18~39세)을 지원합니다."))
                .isNull();
        // "위기청년 ... 회복과 자립을 지원"
        assertThat(ProtectionPhaseClassifier.classify(
                "위기청년(가족돌봄, 고립은둔) 전담 지원사업", "위기청년 조기 발굴 및 회복과 자립을 지원합니다.",
                "가족돌봄청(소)년(13세~34세)"))
                .isNull();
    }

    @Test
    void 보호_중인_아동_대상이면_PRE_TERMINATION() {
        assertThat(ProtectionPhaseClassifier.classify(
                "가정위탁 양육보조금", null, "가정위탁 보호대상아동을 양육하는 위탁부모"))
                .isEqualTo(ProtectionPhase.PRE_TERMINATION);
    }

    @Test
    void 입력이_전부_null이면_null() {
        assertThat(ProtectionPhaseClassifier.classify(null, null, null)).isNull();
    }
}
