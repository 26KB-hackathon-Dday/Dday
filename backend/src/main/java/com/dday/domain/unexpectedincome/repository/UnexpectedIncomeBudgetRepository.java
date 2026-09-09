package com.dday.domain.unexpectedincome.repository;

import com.dday.domain.budget.entity.MonthlyBudget;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 예상 밖 수입을 이번 달 예산에 추가할 때 사용하는
 * MonthlyBudget 전용 Repository.
 *
 * 기존 Budget 도메인의 Repository를 수정하지 않는다.
 */
public interface UnexpectedIncomeBudgetRepository
        extends JpaRepository<MonthlyBudget, Long> {

    /**
     * 현재 월의 예산을 변경하기 위해 잠금을 걸고 조회한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from MonthlyBudget b
            where b.user.userId = :userId
              and b.budgetMonth = :budgetMonth
            """)
    Optional<MonthlyBudget> findCurrentBudgetForUpdate(
            @Param("userId") Long userId,
            @Param("budgetMonth") LocalDate budgetMonth
    );
}