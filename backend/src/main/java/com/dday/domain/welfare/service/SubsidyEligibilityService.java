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

import java.time.LocalDateTime;
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

    /**
     * 홈 조회가 부르는 진입점. 아래 중 하나면 다시 판별한다:
     * <ul>
     *   <li>판별 이력 없음 (첫 조회)</li>
     *   <li>마지막 판별 뒤 유저 프로필이 바뀜 ({@code User.updatedAt} — 보호종료일 입력 등)</li>
     *   <li>마지막 판별 뒤 제도 카탈로그가 바뀜 ({@code welfare_program.updatedAt} — 수집·큐레이션,
     *       특히 {@code protection_phase} 재파생)</li>
     * </ul>
     * 그 외엔 아무것도 안 한다.
     *
     * <p>{@link #evaluate(Long)}는 결정론·멱등이라 불필요하게 한 번 더 돌아도 결과가 같다.
     * "정확히 언제" 재판별할지 조이기보다, 입력(프로필·카탈로그)이 판별 결과보다 최신이면 다시 돌린다.
     * 수집 잡이 돈 뒤 첫 홈 조회 때 유저마다 한 번씩 재판별되는 정도의 비용이다.
     */
    @Transactional
    public void evaluateIfStale(Long userId) {
        LocalDateTime lastEvaluatedAt = eligibilityRepository.findLastEvaluatedAt(userId);
        if (lastEvaluatedAt != null
                && !profileChangedSince(userId, lastEvaluatedAt)
                && !catalogChangedSince(lastEvaluatedAt)) {
            return;
        }
        evaluate(userId);
    }

    private boolean profileChangedSince(Long userId, LocalDateTime since) {
        return userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                .map(User::getUpdatedAt)
                .map(updatedAt -> updatedAt.isAfter(since))
                .orElse(true); // 유저를 못 찾으면 evaluate()가 명확한 예외를 던지게 둔다
    }

    private boolean catalogChangedSince(LocalDateTime since) {
        LocalDateTime lastCatalogChange = welfareProgramRepository.findMaxUpdatedAt();
        return lastCatalogChange != null && lastCatalogChange.isAfter(since);
    }

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
