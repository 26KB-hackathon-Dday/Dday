package com.dday.domain.mydata.repository;

import com.dday.domain.mydata.entity.FinancialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 거래내역 조회.
 *
 * <p><b>목록을 뽑을 때는 반드시 여기 정의된 fetch join 메서드를 쓴다.</b>
 * {@link JpaRepository#findAll()}이나 파생 쿼리(`findByXxx`)로 목록을 뽑으면
 * {@code FinancialTransaction}의 {@code @ManyToOne} 6개가 행마다 지연 로딩되어
 * 쿼리가 폭발한다 (AGENTS.md §4).
 *
 * <p><b>회원으로 거르려면 계좌·카드를 거쳐야 한다.</b> 이 엔티티에는 {@code user_id}가 없다 —
 * 거래는 계좌나 카드에 달리고, 그 둘이 회원을 가리킨다. 그래서 조건이
 * {@code (계좌의 주인 = 나) or (카드의 주인 = 나)}가 된다.
 *
 * <h4>fetch join인데 페이징이 되는 이유</h4>
 * 연관이 전부 <b>to-one</b>이라 조인해도 행 수가 늘지 않는다. 그래서 Hibernate가
 * {@code limit}을 SQL에 그대로 붙인다. to-many(컬렉션)를 fetch join하면 행이 곱해져
 * 페이징을 메모리에서 하게 되고(HHH000104) 데이터가 늘면 그대로 터진다 —
 * <b>이 엔티티에 {@code @OneToMany}를 추가하면 여기 쿼리를 다시 봐야 한다.</b>
 */
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    /**
     * 기간별 거래내역 한 페이지. 목록 화면이 쓰는 기본 조회다.
     *
     * <p>화면이 행마다 그리는 것만 fetch한다 — 계좌·카드(출처), 카테고리·포켓(분류).
     * {@code counterpartyAccount}·{@code originalTransaction}은 일부러 뺐다:
     * 목록에서는 안 쓰는 데다 {@code originalTransaction}은 자기참조라 딸려온 거래가
     * 다시 자기 연관을 물고 온다. 그 둘이 필요한 상세 조회는 {@link #findDetailById}를 쓴다.
     *
     * <p>{@code to}는 포함하지 않는다({@code <}). 월별 조회에서 다음 달 1일 00:00:00.000을
     * 그대로 넘길 수 있어 "그 달의 마지막 밀리초"를 계산하지 않아도 된다.
     */
    @Query(value = """
            select t
            from FinancialTransaction t
            left join fetch t.account a
            left join fetch t.card c
            left join fetch t.category
            left join fetch t.pocket
            where (a.user.userId = :userId or c.user.userId = :userId)
              and t.transactionAt >= :from
              and t.transactionAt < :to
            """,
            countQuery = """
            select count(t)
            from FinancialTransaction t
            left join t.account a
            left join t.card c
            where (a.user.userId = :userId or c.user.userId = :userId)
              and t.transactionAt >= :from
              and t.transactionAt < :to
            """)
    Page<FinancialTransaction> findPageByUserAndPeriod(@Param("userId") Long userId,
                                                       @Param("from") LocalDateTime from,
                                                       @Param("to") LocalDateTime to,
                                                       Pageable pageable);

    /**
     * 특정 포켓의 거래내역 한 페이지. 포켓 상세 화면이 쓴다.
     *
     * <p>포켓이 이미 정해져 있어도 {@code pocket}을 같이 fetch한다 — 안 그러면 행마다
     * 프록시를 건드릴 때 쿼리가 하나씩 더 나간다. 어차피 같은 행이라 비용은 없다.
     */
    @Query(value = """
            select t
            from FinancialTransaction t
            left join fetch t.account
            left join fetch t.card
            left join fetch t.category
            left join fetch t.pocket p
            where p.pocketId = :pocketId
              and t.transactionAt >= :from
              and t.transactionAt < :to
            """,
            countQuery = """
            select count(t)
            from FinancialTransaction t
            left join t.pocket p
            where p.pocketId = :pocketId
              and t.transactionAt >= :from
              and t.transactionAt < :to
            """)
    Page<FinancialTransaction> findPageByPocketAndPeriod(@Param("pocketId") Long pocketId,
                                                         @Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to,
                                                         Pageable pageable);

    /**
     * 상세 조회. 목록이 생략한 {@code counterpartyAccount}·{@code originalTransaction}까지 채운다.
     *
     * <p>{@code originalTransaction}은 한 단계만 fetch한다. 그 원거래의 연관까지 따라가면
     * 조인이 계속 불어나는데, 상세 화면은 "원거래가 있다/그 금액과 일시" 정도만 보여주면 된다.
     */
    @Query("""
            select t
            from FinancialTransaction t
            left join fetch t.account
            left join fetch t.card
            left join fetch t.category
            left join fetch t.pocket
            left join fetch t.counterpartyAccount
            left join fetch t.originalTransaction
            where t.financialTransactionId = :id
            """)
    Optional<FinancialTransaction> findDetailById(@Param("id") Long id);
}
