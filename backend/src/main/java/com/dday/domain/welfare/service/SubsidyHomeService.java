package com.dday.domain.welfare.service;

import com.dday.domain.welfare.dto.response.SubsidyHomeResponse;
import com.dday.domain.welfare.dto.response.SubsidyHomeResponse.SubsidyCard;
import com.dday.domain.welfare.dto.response.SubsidyHomeResponse.Summary;
import com.dday.domain.welfare.entity.ReceivingStatus;
import com.dday.domain.welfare.entity.SupportAmountType;
import com.dday.domain.welfare.entity.UserProgramEligibility;
import com.dday.domain.welfare.entity.UserProgramStatus;
import com.dday.domain.welfare.entity.WelfareProgram;
import com.dday.domain.welfare.repository.UserProgramEligibilityRepository;
import com.dday.domain.welfare.repository.UserProgramStatusRepository;
import com.dday.domain.welfare.repository.WelfareProgramRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 지원금 매칭 홈 대시보드 집계 (SUBSIDY-002/003/004 통합 조회).
 *
 * <p>"확인된 지원" = 자격 있는 제도 전부. 수급 상태로 세 갈래:
 * <ul>
 *   <li>{@code RECEIVING} → 받고 있는 지원</li>
 *   <li>{@code NOT_RECEIVING} → 놓치고 있을 수 있는 지원 (+ 월 환산 합계에 누적)</li>
 *   <li>그 외({@code UNKNOWN}/status 행 없음/{@code LIKELY_RECEIVING}) → 확인 필요한 지원</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SubsidyHomeService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final UserProgramEligibilityRepository eligibilityRepository;
    private final UserProgramStatusRepository statusRepository;
    private final WelfareProgramRepository welfareProgramRepository;
    private final SubsidyEligibilityService eligibilityService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SubsidyHomeResponse getHome(Long userId) {
        // 첫 조회거나, 마지막 판별 뒤 프로필이 바뀌었으면(보호종료일 입력 등) 먼저 재평가한다.
        eligibilityService.evaluateIfStale(userId);

        List<UserProgramEligibility> eligible = eligibilityRepository.findByIdUserIdAndEligibleTrue(userId);
        List<String> programIds = eligible.stream().map(e -> e.getId().getProgramId()).toList();

        Map<String, UserProgramStatus> statusByProgram = programIds.isEmpty() ? Map.of()
                : statusRepository.findByIdUserIdAndIdProgramIdIn(userId, programIds).stream()
                        .collect(Collectors.toMap(s -> s.getId().getProgramId(), Function.identity()));
        Map<String, WelfareProgram> programById = programIds.isEmpty() ? Map.of()
                : welfareProgramRepository.findByServIdIn(programIds).stream()
                        .collect(Collectors.toMap(WelfareProgram::getServId, Function.identity()));

        List<SubsidyCard> receiving = new ArrayList<>();
        List<SubsidyCard> missing = new ArrayList<>();
        List<SubsidyCard> reviewQueue = new ArrayList<>();
        long missedMonthlySum = 0;

        for (UserProgramEligibility e : eligible) {
            WelfareProgram program = programById.get(e.getId().getProgramId());
            if (program == null) {
                continue; // 자격은 있으나 제도가 비공개로 바뀐 경우 — 스킵
            }
            UserProgramStatus status = statusByProgram.get(e.getId().getProgramId());
            ReceivingStatus rs = status == null ? ReceivingStatus.UNKNOWN : status.getReceivingStatus();
            Long monthly = monthlyEquivalent(program);
            SubsidyCard card = SubsidyCard.of(program, e, monthly, parseCriteria(e.getMatchedCriteria()));

            switch (rs) {
                case RECEIVING -> receiving.add(card);
                case NOT_RECEIVING -> {
                    missing.add(card);
                    if (monthly != null) {
                        missedMonthlySum += monthly;
                    }
                }
                default -> reviewQueue.add(card); // UNKNOWN, LIKELY_RECEIVING
            }
        }

        LocalDateTime evaluatedAt = eligibilityRepository.findLastEvaluatedAt(userId);

        return SubsidyHomeResponse.builder()
                .summary(Summary.builder()
                        .confirmed(eligible.size())
                        .receiving(receiving.size())
                        .actionNeeded(missing.size())
                        .needsReview(reviewQueue.size())
                        .totalMonthlyEquivalentAmount(missedMonthlySum)
                        .evaluatedAt(evaluatedAt)
                        .build())
                .receivingList(receiving)
                .missing(missing)
                .reviewQueue(reviewQueue)
                .build();
    }

    /** 월 환산 지원액. {@code MONTHLY}는 그대로, {@code FIXED}는 개월 수로 나눔. 나머지·미상은 {@code null}. */
    private Long monthlyEquivalent(WelfareProgram p) {
        Long amount = p.getSupportAmount();
        SupportAmountType type = p.getSupportAmountType();
        if (amount == null || type == null) {
            return null;
        }
        return switch (type) {
            case MONTHLY -> amount;
            case FIXED -> {
                Integer months = p.getSupportDurationMonths();
                yield (months == null || months <= 0) ? null : amount / months;
            }
            default -> null; // LIMIT, SEMIANNUAL — 월 환산 정의 미정
        };
    }

    private List<String> parseCriteria(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception e) {
            return List.of();
        }
    }
}
