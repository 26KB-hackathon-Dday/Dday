package com.dday.domain.welfare.service;

import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.repository.UserRepository;
import com.dday.domain.welfare.collector.eligibility.EligibilityEvaluator;
import com.dday.domain.welfare.collector.eligibility.EligibilityResult;
import com.dday.domain.welfare.dto.WelfareErrorCode;
import com.dday.domain.welfare.entity.UserProgramEligibility;
import com.dday.domain.welfare.entity.WelfareProgram;
import com.dday.domain.welfare.repository.UserProgramEligibilityRepository;
import com.dday.domain.welfare.repository.WelfareProgramRepository;
import com.dday.global.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 유저별 제도 자격 재판별 (SUBSIDY-002).
 *
 * <p>공개 대상 제도({@link WelfareProgram#isPubliclyVisible()}) 전부를 순회하며
 * {@link EligibilityEvaluator}로 판정해 {@code user_program_eligibility}에 upsert한다.
 * 사용자 수급 상태({@code user_program_status})는 건드리지 않는다 — 소유자가 다르다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubsidyEligibilityService {

    private final UserRepository userRepository;
    private final WelfareProgramRepository welfareProgramRepository;
    private final UserProgramEligibilityRepository eligibilityRepository;
    private final EligibilityEvaluator evaluator;
    private final ObjectMapper objectMapper;

    @Transactional
    public void evaluate(Long userId) {
        User user = userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(WelfareErrorCode.SUBSIDY_USER_NOT_FOUND));

        List<WelfareProgram> programs = welfareProgramRepository.findAll().stream()
                .filter(WelfareProgram::isPubliclyVisible)
                .toList();

        Map<String, UserProgramEligibility> rows = eligibilityRepository.findByIdUserId(userId).stream()
                .collect(Collectors.toMap(e -> e.getId().getProgramId(), Function.identity()));

        for (WelfareProgram program : programs) {
            EligibilityResult result = evaluator.evaluate(user, program);
            UserProgramEligibility row = rows.computeIfAbsent(
                    program.getServId(), pid -> new UserProgramEligibility(userId, pid));
            row.applyEvaluation(result.eligible(), toJson(result.matchedCriteria()), result.ineligibleReason());
        }

        eligibilityRepository.saveAll(rows.values());
        log.debug("자격 재판별 user={} 제도 {}건", userId, programs.size());
    }

    private String toJson(List<String> criteria) {
        try {
            return objectMapper.writeValueAsString(criteria);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
