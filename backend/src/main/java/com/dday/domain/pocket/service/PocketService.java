package com.dday.domain.pocket.service;

import com.dday.domain.pocket.dto.PocketErrorCode;
import com.dday.domain.pocket.dto.response.PocketResponse;
import com.dday.domain.pocket.entity.Pocket;
import com.dday.domain.pocket.repository.PocketRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 인터페이스를 두지 않는다 — 구현체가 하나뿐인데 인터페이스를 만들면 파일만 두 배가 된다
 * (AGENTS.md §7).
 */
@Service
@RequiredArgsConstructor
public class PocketService {

    private final PocketRepository pocketRepository;

    /**
     * 포켓 목록을 소진 현황과 함께 돌려준다.
     *
     * <p>정렬은 DB가 아니라 여기서 한다. {@code type}이 문자열로 저장돼 있어 DB 정렬은
     * 알파벳순(ASSET, EMERGENCY, HOUSING, LIVING)이 되는데, 화면에 필요한 건
     * <b>주거 → 생활 → 비상 → 자산형성</b>이라는 우선순위이기 때문이다.
     * 그 순서는 {@code PocketType}의 선언 순서가 들고 있다.
     */
    @Transactional(readOnly = true)
    public List<PocketResponse> findAll() {
        return pocketRepository.findAll().stream()
                .sorted(Comparator.comparing(pocket -> pocket.getType().ordinal()))
                .map(PocketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PocketResponse findById(Long pocketId) {
        Pocket pocket = pocketRepository.findById(pocketId)
                .orElseThrow(() -> new BusinessException(PocketErrorCode.POCKET_NOT_FOUND));
        return PocketResponse.from(pocket);
    }
}
