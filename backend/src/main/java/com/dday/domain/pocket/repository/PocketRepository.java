package com.dday.domain.pocket.repository;

import com.dday.domain.pocket.entity.Pocket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PocketRepository extends JpaRepository<Pocket, Long> {

    /**
     * 한 사용자의 포켓 전부(고정 4개).
     *
     * <p>정렬을 걸지 않는다. {@code pocket_type}이 {@code EnumType.STRING}이라 DB 정렬은
     * 이름 알파벳순(EMERGENCY, ESSENTIAL…)이 되어버리는데, 화면에 필요한 건 그 순서가 아니라
     * <b>돈이 확보돼야 하는 우선순위</b>라 서비스에서 enum 선언 순서로 다시 잡는다.
     */
    List<Pocket> findAllByUserUserId(Long userId);

    /**
     * 남의 포켓을 id로 찍어 조회하는 것을 막으려고 소유자까지 함께 건다.
     * 소유자가 다르면 {@code empty}가 나오고, 서비스는 그걸 "없음"으로 처리한다 —
     * "권한 없음"으로 답하면 그 id의 포켓이 존재한다는 사실이 새어나간다.
     */
    Optional<Pocket> findByPocketIdAndUserUserId(Long pocketId, Long userId);
}
