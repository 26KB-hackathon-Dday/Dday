package com.dday.domain.pocket.repository;

import com.dday.domain.pocket.entity.Pocket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** 사용자와 유형을 동시에 제한해 URL의 포켓 유형이 실제 사용자 포켓인지 확인한다. */
    Optional<Pocket> findByUserUserIdAndPocketType(
            Long userId, com.dday.domain.pocket.entity.PocketType pocketType);

    /**
     * 해당 유형의 포켓이 없을 때만 생성한다.
     *
     * <p>서비스의 사전 조회는 불필요한 삽입을 줄이는 용도이고, 실제 동시성 안전성은
     * {@code (user_id, pocket_type)} 유일 제약과 MySQL {@code insert ignore}가 보장한다.
     * 이미 존재하면 0, 새로 생성하면 1을 반환한다.
     */
    @Modifying
    @Query(value = """
            insert ignore into pocket (user_id, pocket_type, pocket_name, created_at)
            values (:userId, :pocketType, :pocketName, now())
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId,
                       @Param("pocketType") String pocketType,
                       @Param("pocketName") String pocketName);
}
