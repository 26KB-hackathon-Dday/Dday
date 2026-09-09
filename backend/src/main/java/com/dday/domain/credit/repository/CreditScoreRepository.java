package com.dday.domain.credit.repository;

import com.dday.domain.credit.entity.CreditScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 신용점수 기록 조회.
 *
 * <p>{@code CreditScore}는 덮어쓰지 않고 쌓이는 이력이라, 회원별 조회는 언제나
 * "최신 몇 건"이다. {@code (user_id, updated_at)} 인덱스가 그 조회를 받쳐준다.
 */
public interface CreditScoreRepository extends JpaRepository<CreditScore, Long> {

    /**
     * 최근 여섯 건. <b>화면에 뿌리는 건 다섯 건인데 여섯 건을 읽는다</b> —
     * 다섯 번째 항목의 변동폭을 계산하려면 그 직전 기록이 한 건 더 필요해서다.
     *
     * <p>{@code updatedAt}만으로 정렬하지 않는다. 같은 시각에 들어온 기록의 순서가 흔들리면
     * 변동폭의 부호가 뒤집히므로 PK를 보조 정렬키로 건다.
     */
    List<CreditScore> findTop6ByUserUserIdOrderByUpdatedAtDescCreditScoreIdDesc(Long userId);

    /**
     * 가장 최근 기록 한 건. 정렬 기준은 위와 같다 — 같은 시각의 기록이 섞이면 "최신"이 흔들린다.
     */
    Optional<CreditScore> findFirstByUserUserIdOrderByUpdatedAtDescCreditScoreIdDesc(Long userId);
}
