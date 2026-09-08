package com.dday.domain.credit.service;

import com.dday.domain.credit.dto.response.CreditScoreHistoryResponse;
import com.dday.domain.credit.dto.response.CreditScoreItemResponse;
import com.dday.domain.credit.entity.CreditScore;
import com.dday.domain.credit.repository.CreditScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 신용점수 이력을 조회하는 애플리케이션 서비스다.
 *
 * <p>이 기능의 핵심은 점수 자체가 아니라 <b>변화</b>다. 그래서 조회 결과를 그대로 내리지 않고
 * 항목마다 직전 기록과의 증감을 붙여서 내려준다.
 *
 * <p>인터페이스를 두지 않는다 — 구현체가 하나뿐인데 인터페이스를 만들면 파일만 두 배가 된다
 * (AGENTS.md §7).
 */
@Service
@RequiredArgsConstructor
public class CreditScoreService {

    /** 화면에 뿌리는 이력 개수. */
    private static final int RECENT_SIZE = 5;

    private final CreditScoreRepository creditScoreRepository;

    /**
     * 최근 {@value #RECENT_SIZE}건을 최신순으로 돌려준다.
     *
     * <p>레포지토리에서는 한 건 더 많은 여섯 건을 읽는다. 마지막 항목의 변동폭을 채우려면
     * 목록 밖의 직전 기록이 필요해서다. 그 여섯 번째 기록도 없으면 마지막 항목의 변동폭은
     * {@code null}이 되고, 프론트는 화살표를 그리지 않는다.
     */
    @Transactional(readOnly = true)
    public CreditScoreHistoryResponse findRecent(Long userId) {
        List<CreditScore> records =
                creditScoreRepository.findTop6ByUserUserIdOrderByUpdatedAtDescCreditScoreIdDesc(userId);

        List<CreditScoreItemResponse> items = new ArrayList<>();
        for (int i = 0; i < Math.min(records.size(), RECENT_SIZE); i++) {
            items.add(CreditScoreItemResponse.of(records.get(i), diffFromPrevious(records, i)));
        }
        return CreditScoreHistoryResponse.of(items);
    }

    /** 목록은 최신순이라 "직전 기록"은 다음 원소다. 그게 없으면 비교 대상이 없다는 뜻이다. */
    private Integer diffFromPrevious(List<CreditScore> records, int index) {
        int previous = index + 1;
        if (previous >= records.size()) {
            return null;
        }
        return records.get(index).getScore() - records.get(previous).getScore();
    }
}
