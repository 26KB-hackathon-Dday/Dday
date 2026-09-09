package com.dday.domain.credit.repository;

import com.dday.domain.credit.entity.CreditScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** 이 회원에게 이력이 한 건이라도 있는지. 데모 이력을 두 번 붙이지 않으려고 본다. */
    boolean existsByUserUserId(Long userId);

    /**
     * 다른 회원의 이력을 통째로 복제한다. 시연에서 누가 가입하든 같은 그래프가 보이게 하려고
     * {@code CreditDemoProvisioner}가 부른다. 새로 넣은 건수를 돌려준다.
     *
     * <p><b>JPA가 아니라 네이티브 SQL을 쓰는 이유는 {@code updated_at} 때문이다.</b>
     * 이 컬럼은 {@code @UpdateTimestamp}라, 엔티티로 저장하면 Hibernate가 무조건 '지금'으로
     * 덮어쓴다. 그런데 화면은 이 값을 각 기록의 <b>월 표시</b>로 쓰므로(신용점수 이력 표),
     * 여섯 건이 전부 오늘 날짜가 되면 같은 달이 여섯 줄 뜬다. 원본의 시각을 그대로 옮기려면
     * 엔티티 생명주기를 우회하는 수밖에 없다.
     *
     * <p>{@code credit_score}에는 유일 제약이 없어 이 문장만으로는 멱등하지 않다.
     * 중복 방지는 호출부의 {@link #existsByUserUserId} 검사가 맡는다.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into credit_score (user_id, agency, score, created_at, updated_at)
            select :userId, t.agency, t.score, t.created_at, t.updated_at
              from credit_score t
             where t.user_id = :templateUserId
            """, nativeQuery = true)
    int copyHistoryFrom(@Param("userId") Long userId,
                        @Param("templateUserId") Long templateUserId);
}
