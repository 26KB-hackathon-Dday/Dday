package com.dday.domain.welfare.entity;

import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.collector.Classification;
import com.dday.domain.welfare.collector.Classification.Disposition;
import com.dday.domain.welfare.collector.validation.WelfareIssue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WelfareProgramTest {

    @Test
    void 검증_전이거나_OK면_노출_NEEDS_REVIEW면_숨김() {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);
        assertThat(p.isPubliclyVisible()).isTrue(); // curationStatus == null

        p.applyCurationReview(List.of());
        assertThat(p.isPubliclyVisible()).isTrue(); // OK

        ReflectionTestUtils.setField(p, "curationStatus", CurationStatus.NEEDS_REVIEW);
        assertThat(p.isPubliclyVisible()).isFalse();
    }

    @Test
    void MANUAL_CURATION이면_검토중이어도_노출한다() {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);
        ReflectionTestUtils.setField(p, "curationStatus", CurationStatus.NEEDS_REVIEW);
        ReflectionTestUtils.setField(p, "source", ProgramSource.MANUAL_CURATION);

        assertThat(p.isPubliclyVisible()).isTrue();
    }

    @Test
    void MANUAL_CURATION_행은_재수집이_큐레이션_필드를_안_덮는다() {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);
        ReflectionTestUtils.setField(p, "source", ProgramSource.MANUAL_CURATION);
        ReflectionTestUtils.setField(p, "servNm", "관리자가 고친 이름");
        ReflectionTestUtils.setField(p, "category", "주거");

        WelfareListItem item = BeanUtils.instantiateClass(WelfareListItem.class);
        ReflectionTestUtils.setField(item, "servNm", "API 원본 이름");
        ReflectionTestUtils.setField(item, "rawXml", "<raw/>");
        LocalDateTime now = LocalDateTime.now();

        p.applyCollection(item, new Classification(Disposition.STORED, YouthStatus.AUTO_APPROVED, 3, "R2"), now);

        assertThat(p.getServNm()).isEqualTo("관리자가 고친 이름"); // 안 덮임
        assertThat(p.getCategory()).isEqualTo("주거");           // 안 덮임
        assertThat(p.getCollectedAt()).isEqualTo(now);           // 신선도는 갱신
        assertThat(p.getRawListXml()).isEqualTo("<raw/>");       // 원문은 갱신
    }

    @Test
    void MANUAL_CURATION_행은_상세보강이_파싱값을_안_덮는다() {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);
        ReflectionTestUtils.setField(p, "source", ProgramSource.MANUAL_CURATION);
        ReflectionTestUtils.setField(p, "targetDescription", "관리자가 정리한 지원대상");
        ReflectionTestUtils.setField(p, "supportAmount", 300_000L);

        p.applyDetail(999_999L, 24, "2026", "API가 준 잡탕 텍스트",
                "채널", "https://x", "010", "<detail/>");

        assertThat(p.getTargetDescription()).isEqualTo("관리자가 정리한 지원대상");
        assertThat(p.getSupportAmount()).isEqualTo(300_000L);
        assertThat(p.getRawDetailXml()).isEqualTo("<detail/>"); // 원문만 갱신
    }

    @Test
    void 이슈가_없으면_OK로_기록하고_사유는_비운다() {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);

        p.applyCurationReview(List.of());

        assertThat(p.getCurationStatus()).isEqualTo(CurationStatus.OK);
        assertThat(p.getCurationIssues()).isNull();
    }

    @Test
    void 이슈가_있으면_NEEDS_REVIEW로_기록하고_이름을_쉼표로_잇는다() {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);

        p.applyCurationReview(List.of(WelfareIssue.CASH_WITHOUT_AMOUNT, WelfareIssue.TARGET_MISSING));

        assertThat(p.getCurationStatus()).isEqualTo(CurationStatus.NEEDS_REVIEW);
        assertThat(p.getCurationIssues()).isEqualTo("CASH_WITHOUT_AMOUNT,TARGET_MISSING");
    }

    @Test
    void 재검증에서_통과하면_이전_사유를_지운다() {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);
        p.applyCurationReview(List.of(WelfareIssue.CATEGORY_MISSING));

        p.applyCurationReview(List.of());

        assertThat(p.getCurationStatus()).isEqualTo(CurationStatus.OK);
        assertThat(p.getCurationIssues()).isNull();
    }
}
