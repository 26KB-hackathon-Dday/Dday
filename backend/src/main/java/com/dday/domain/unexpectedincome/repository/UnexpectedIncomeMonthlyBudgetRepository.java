package com.dday.domain.unexpectedincome.repository;

import com.dday.domain.budget.entity.MonthlyBudget;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UnexpectedIncomeMonthlyBudgetRepository
        extends JpaRepository<MonthlyBudget, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from MonthlyBudget b
            where b.user.userId = :userId
              and b.budgetMonth = :budgetMonth
            """)
    Optional<MonthlyBudget> findForUpdate(
            @Param("userId") Long userId,
            @Param("budgetMonth") LocalDate budgetMonth
    );
}