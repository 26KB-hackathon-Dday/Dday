package com.dday.domain.welfare.service;

import com.dday.domain.welfare.dto.WelfareErrorCode;
import com.dday.domain.welfare.dto.response.WelfareProgramDetailResponse;
import com.dday.domain.welfare.dto.response.WelfareProgramSummaryResponse;
import com.dday.domain.welfare.dto.response.WelfareReviewItemResponse;
import com.dday.domain.welfare.entity.CurationStatus;
import com.dday.domain.welfare.entity.ProgramSource;
import com.dday.domain.welfare.entity.WelfareProgram;
import com.dday.domain.welfare.repository.WelfareProgramRepository;
import com.dday.global.common.dto.PageResponse;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 제도 마스터 읽기 (SUBSIDY-009 목록·검색, SUBSIDY-005 상세).
 *
 * <p>공개 데이터라 인증이 필요 없다. 자격 판별(SUBSIDY-002)이 붙기 전이라
 * "내가 받을 가능성 있는 것만" 필터는 여기 없다.
 */
@Service
@RequiredArgsConstructor
public class WelfareProgramService {

    private final WelfareProgramRepository welfareProgramRepository;

    /**
     * 검색어·카테고리로 제도를 조회한다. 둘 다 optional이고 서로 독립이다.
     * 빈 문자열은 "조건 없음"으로 본다.
     */
    @Transactional(readOnly = true)
    public PageResponse<WelfareProgramSummaryResponse> search(String q, String category, Pageable pageable) {
        Page<WelfareProgram> page = welfareProgramRepository.search(
                blankToNull(q), blankToNull(category), pageable);
        return PageResponse.of(page, WelfareProgramSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public WelfareProgramDetailResponse getByProgramId(String programId) {
        WelfareProgram program = welfareProgramRepository.findByServId(programId)
                .filter(WelfareProgram::isPubliclyVisible)
                .orElseThrow(() -> new BusinessException(WelfareErrorCode.PROGRAM_NOT_FOUND));
        return WelfareProgramDetailResponse.from(program);
    }

    /**
     * 리뷰 큐 — 검증에서 걸렸고 아직 아무도 손 안 댄 행. 관리자가 DB에서 값을 고친 뒤
     * {@code source}를 {@code MANUAL_CURATION}으로 바꾸면 큐에서 빠지고 재수집에도 안 덮인다.
     */
    @Transactional(readOnly = true)
    public List<WelfareReviewItemResponse> getReviewQueue() {
        return welfareProgramRepository
                .findByCurationStatusAndSourceOrderByServId(CurationStatus.NEEDS_REVIEW, ProgramSource.API_CANDIDATE)
                .stream()
                .map(WelfareReviewItemResponse::from)
                .toList();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
