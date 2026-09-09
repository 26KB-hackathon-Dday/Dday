package com.dday.domain.budget.repository;

import com.dday.domain.budget.entity.MonthlyBudget;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface MonthlyBudgetRepository
        extends JpaRepository<MonthlyBudget, Long> {

    /**
     * 사용자 + 기준 월로 월 예산 조회.
     */
    Optional<MonthlyBudget> findByUserUserIdAndBudgetMonth(
            Long userId,
            LocalDate budgetMonth
    );

    /**
     * 예상 외 입금처럼 실제 예산 금액을 수정할 때 사용.
     *
     * 같은 월 예산을 동시에 수정하는 상황을 막기 위해
     * PESSIMISTIC_WRITE 잠금을 건다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from MonthlyBudget b
            where b.user.userId = :userId
              and b.budgetMonth = :budgetMonth
              and b.budgetStatus =
                  com.dday.domain.budget.entity.BudgetStatus.CONFIRMED
            """)
    Optional<MonthlyBudget> findConfirmedForUpdate(
            @Param("userId") Long userId,
            @Param("budgetMonth") LocalDate budgetMonth
    );
}