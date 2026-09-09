package com.dday.domain.budgetadjustment.repository;

import com.dday.domain.budget.entity.MonthlyBudget;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface BudgetAdjustmentMonthlyBudgetRepository
        extends JpaRepository<MonthlyBudget, Long> {

    @Query("""
            select budget
            from MonthlyBudget budget
            where budget.user.userId = :userId
              and budget.budgetMonth = :budgetMonth
            """)
    Optional<MonthlyBudget> findCurrent(
            @Param("userId") Long userId,
            @Param("budgetMonth") LocalDate budgetMonth
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select budget
            from MonthlyBudget budget
            where budget.user.userId = :userId
              and budget.budgetMonth = :budgetMonth
            """)
    Optional<MonthlyBudget> findCurrentForUpdate(
            @Param("userId") Long userId,
            @Param("budgetMonth") LocalDate budgetMonth
    );
}