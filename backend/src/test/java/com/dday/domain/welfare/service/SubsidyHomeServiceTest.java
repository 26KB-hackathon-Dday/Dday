package com.dday.domain.welfare.service;

import com.dday.domain.welfare.dto.response.SubsidyHomeResponse;
import com.dday.domain.welfare.entity.ReceivingStatus;
import com.dday.domain.welfare.entity.SupportAmountType;
import com.dday.domain.welfare.entity.UserProgramEligibility;
import com.dday.domain.welfare.entity.UserProgramStatus;
import com.dday.domain.welfare.entity.WelfareProgram;
import com.dday.domain.welfare.repository.UserProgramEligibilityRepository;
import com.dday.domain.welfare.repository.UserProgramStatusRepository;
import com.dday.domain.welfare.repository.WelfareProgramRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SubsidyHomeServiceTest {

    private static final long USER_ID = 7L;

    @Mock UserProgramEligibilityRepository eligibilityRepository;
    @Mock UserProgramStatusRepository statusRepository;
    @Mock WelfareProgramRepository welfareProgramRepository;
    @Mock SubsidyEligibilityService eligibilityService;

    SubsidyHomeService service;

    @BeforeEach
    void setUp() {
        service = new SubsidyHomeService(eligibilityRepository, statusRepository,
                welfareProgramRepository, eligibilityService, new ObjectMapper());
    }

    @Test
    void 홈_조회는_먼저_stale_재평가를_트리거한다() {
        given(eligibilityRepository.findByIdUserIdAndEligibleTrue(USER_ID)).willReturn(List.of());

        service.getHome(USER_ID);

        org.mockito.Mockito.verify(eligibilityService).evaluateIfStale(USER_ID);
    }

    @Test
    void 수급상태로_세_버킷으로_나누고_월환산_합계를_낸다() {
        given(eligibilityRepository.findByIdUserIdAndEligibleTrue(USER_ID)).willReturn(List.of(
                eligibility("A"), eligibility("B"), eligibility("C"), eligibility("D")));
        given(welfareProgramRepository.findByServIdIn(any())).willReturn(List.of(
                program("A", 500_000L, SupportAmountType.MONTHLY, null),
                program("B", 200_000L, SupportAmountType.MONTHLY, null),
                program("C", 1_200_000L, SupportAmountType.FIXED, 12),   // 월 환산 100,000
                program("D", 300_000L, SupportAmountType.MONTHLY, null)));
        given(statusRepository.findByIdUserIdAndIdProgramIdIn(anyLong(), any())).willReturn(List.of(
                status("A", ReceivingStatus.RECEIVING),
                status("B", ReceivingStatus.NOT_RECEIVING),
                status("C", ReceivingStatus.NOT_RECEIVING)));
        // D는 status 행 없음 → 확인 필요
        LocalDateTime lastEval = LocalDateTime.of(2026, 9, 8, 20, 13);
        given(eligibilityRepository.findLastEvaluatedAt(USER_ID)).willReturn(lastEval);

        SubsidyHomeResponse res = service.getHome(USER_ID);

        assertThat(res.getSummary().getEvaluatedAt()).isEqualTo(lastEval);
        assertThat(res.getSummary().getConfirmed()).isEqualTo(4);
        assertThat(res.getReceivingList()).extracting(SubsidyHomeResponse.SubsidyCard::getProgramId)
                .containsExactly("A");
        assertThat(res.getMissing()).extracting(SubsidyHomeResponse.SubsidyCard::getProgramId)
                .containsExactlyInAnyOrder("B", "C");
        assertThat(res.getReviewQueue()).extracting(SubsidyHomeResponse.SubsidyCard::getProgramId)
                .containsExactly("D");
        // 놓치고 있는 것만: B(200,000) + C(1,200,000/12 = 100,000)
        assertThat(res.getSummary().getTotalMonthlyEquivalentAmount()).isEqualTo(300_000L);
    }

    @Test
    void 자격_있는_제도가_없으면_전부_빈_목록() {
        given(eligibilityRepository.findByIdUserIdAndEligibleTrue(USER_ID)).willReturn(List.of());

        SubsidyHomeResponse res = service.getHome(USER_ID);

        assertThat(res.getSummary().getConfirmed()).isZero();
        assertThat(res.getReceivingList()).isEmpty();
        assertThat(res.getMissing()).isEmpty();
        assertThat(res.getReviewQueue()).isEmpty();
    }

    private static UserProgramEligibility eligibility(String programId) {
        var e = new UserProgramEligibility(USER_ID, programId);
        e.applyEvaluation(true, "[\"protectionEndDate\"]", null);
        return e;
    }

    private static UserProgramStatus status(String programId, ReceivingStatus rs) {
        var s = new UserProgramStatus(USER_ID, programId);
        s.updateReceiving(rs, com.dday.domain.welfare.entity.DetectionSource.USER_INPUT, null);
        return s;
    }

    private static WelfareProgram program(String servId, Long amount, SupportAmountType type, Integer months) {
        WelfareProgram p = BeanUtils.instantiateClass(WelfareProgram.class);
        ReflectionTestUtils.setField(p, "servId", servId);
        ReflectionTestUtils.setField(p, "servNm", "제도 " + servId);
        ReflectionTestUtils.setField(p, "category", "생활");
        ReflectionTestUtils.setField(p, "supportAmount", amount);
        ReflectionTestUtils.setField(p, "supportAmountType", type);
        ReflectionTestUtils.setField(p, "supportDurationMonths", months);
        return p;
    }
}
