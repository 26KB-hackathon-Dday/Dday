package com.dday.domain.unexpectedincome.repository;

import com.dday.domain.budget.entity.MonthlyPocketBudget;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 예상 밖 수입을 포켓별로 배분할 때 사용하는
 * MonthlyPocketBudget 전용 Repository.
 */
public interface UnexpectedIncomePocketBudgetRepository
        extends JpaRepository<MonthlyPocketBudget, Long> {

    /**
     * 해당 월의 포켓 예산을 잠그고 조회한다.
     *
     * 로그인 사용자의 포켓인지도 같이 확인한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from MonthlyPocketBudget b
            join fetch b.pocket p
            where b.monthlyBudget.monthlyBudgetId = :monthlyBudgetId
              and p.user.userId = :userId
            """)
    List<MonthlyPocketBudget> findAllForUpdate(
            @Param("monthlyBudgetId") Long monthlyBudgetId,
            @Param("userId") Long userId
    );
}