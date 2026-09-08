package com.dday.domain.welfare.service;

import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.domain.welfare.collector.eligibility.EligibilityEvaluator;
import com.dday.domain.welfare.collector.eligibility.EligibilityResult;
import com.dday.domain.welfare.entity.ProgramSource;
import com.dday.domain.welfare.entity.WelfareProgram;
import com.dday.domain.welfare.repository.UserProgramEligibilityRepository;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubsidyEligibilityServiceTest {

    private static final long USER_ID = 7L;
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 20, 0, 0);
    private static final LocalDateTime T1 = LocalDateTime.of(2026, 9, 1, 0, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 9, 8, 0, 0);

    @Mock UserRepository userRepository;
    @Mock WelfareProgramRepository welfareProgramRepository;
    @Mock UserProgramEligibilityRepository eligibilityRepository;
    @Mock EligibilityEvaluator evaluator;

    SubsidyEligibilityService service;

    @BeforeEach
    void setUp() {
        service = new SubsidyEligibilityService(userRepository, welfareProgramRepository,
                eligibilityRepository, evaluator, new ObjectMapper());
    }

    @Test
    void 판별_이력이_없으면_판별한다() {
        given(eligibilityRepository.findLastEvaluatedAt(USER_ID)).willReturn(null);
        stubEvaluateRun(T2);

        service.evaluateIfStale(USER_ID);

        verify(eligibilityRepository).saveAll(any());
    }

    @Test
    void 마지막_판별_뒤_프로필이_바뀌었으면_다시_판별한다() {
        given(eligibilityRepository.findLastEvaluatedAt(USER_ID)).willReturn(T1);
        stubEvaluateRun(T2); // user.updatedAt = T2 > T1

        service.evaluateIfStale(USER_ID);

        verify(eligibilityRepository).saveAll(any());
    }

    @Test
    void 마지막_판별_뒤_제도_카탈로그가_바뀌었으면_다시_판별한다() {
        given(eligibilityRepository.findLastEvaluatedAt(USER_ID)).willReturn(T1);
        given(welfareProgramRepository.findMaxUpdatedAt()).willReturn(T2); // 카탈로그가 T1 이후 변경
        stubEvaluateRun(T0); // 프로필은 그대로(T0 < T1)

        service.evaluateIfStale(USER_ID);

        verify(eligibilityRepository).saveAll(any());
    }

    @Test
    void 프로필도_카탈로그도_그대로면_건너뛴다() {
        given(eligibilityRepository.findLastEvaluatedAt(USER_ID)).willReturn(T2);
        given(userRepository.findByUserIdAndStatus(USER_ID, UserStatus.ACTIVE))
                .willReturn(Optional.of(user(T1)));
        given(welfareProgramRepository.findMaxUpdatedAt()).willReturn(T1);

        service.evaluateIfStale(USER_ID);

        verify(welfareProgramRepository, never()).findAll();
        verify(eligibilityRepository, never()).saveAll(any());
    }

    /** evaluate() 본체가 끝까지 돌도록 최소 스텁 — 프로그램 1건, 기존 판별 행 없음. */
    private void stubEvaluateRun(LocalDateTime userUpdatedAt) {
        given(userRepository.findByUserIdAndStatus(USER_ID, UserStatus.ACTIVE))
                .willReturn(Optional.of(user(userUpdatedAt)));
        WelfareProgram program = BeanUtils.instantiateClass(WelfareProgram.class);
        ReflectionTestUtils.setField(program, "servId", "WLF_A");
        ReflectionTestUtils.setField(program, "source", ProgramSource.API_CANDIDATE);
        given(welfareProgramRepository.findAll()).willReturn(List.of(program));
        given(eligibilityRepository.findByIdUserId(USER_ID)).willReturn(List.of());
        given(evaluator.evaluate(any(), any())).willReturn(EligibilityResult.eligible(List.of()));
    }

    private static User user(LocalDateTime updatedAt) {
        User u = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(u, "userId", USER_ID);
        ReflectionTestUtils.setField(u, "updatedAt", updatedAt);
        return u;
    }
}
