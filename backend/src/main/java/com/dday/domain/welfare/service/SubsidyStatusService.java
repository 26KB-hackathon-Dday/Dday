package com.dday.domain.welfare.service;

import com.dday.domain.welfare.dto.WelfareErrorCode;
import com.dday.domain.welfare.dto.response.ReceivingStatusResponse;
import com.dday.domain.welfare.entity.DetectionSource;
import com.dday.domain.welfare.entity.ReceivingStatus;
import com.dday.domain.welfare.entity.UserProgramEligibility;
import com.dday.domain.welfare.entity.UserProgramId;
import com.dday.domain.welfare.entity.UserProgramStatus;
import com.dday.domain.welfare.repository.UserProgramEligibilityRepository;
import com.dday.domain.welfare.repository.UserProgramStatusRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수급 여부 등록/수정 (SUBSIDY-003).
 *
 * <p>사용자가 직접 체크한 값만 받는다 — {@code detection_source = USER_INPUT}.
 * 자격이 확인되지 않은({@code eligible != true}) 제도에는 상태를 못 남긴다(422).
 */
@Service
@RequiredArgsConstructor
public class SubsidyStatusService {

    private final UserProgramEligibilityRepository eligibilityRepository;
    private final UserProgramStatusRepository statusRepository;

    @Transactional
    public ReceivingStatusResponse updateReceiving(Long userId, String programId, ReceivingStatus status) {
        UserProgramId id = new UserProgramId(userId, programId);

        eligibilityRepository.findById(id)
                .filter(UserProgramEligibility::isEligible)
                .orElseThrow(() -> new BusinessException(WelfareErrorCode.INELIGIBLE_PROGRAM));

        UserProgramStatus row = statusRepository.findById(id)
                .orElseGet(() -> new UserProgramStatus(userId, programId));
        row.updateReceiving(status, DetectionSource.USER_INPUT, null);

        return ReceivingStatusResponse.from(statusRepository.save(row));
    }
}
