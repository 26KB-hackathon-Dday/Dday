package com.dday.domain.budgetadjustment.repository;

import com.dday.domain.budget.entity.MonthlyPocketBudget;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BudgetAdjustmentPocketBudgetRepository
        extends JpaRepository<MonthlyPocketBudget, Long> {

    @Query("""
            select budget
            from MonthlyPocketBudget budget
            join fetch budget.pocket pocket
            where budget.monthlyBudget.monthlyBudgetId = :monthlyBudgetId
              and pocket.user.userId = :userId
            """)
    List<MonthlyPocketBudget> findAllCurrent(
            @Param("monthlyBudgetId") Long monthlyBudgetId,
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select budget
            from MonthlyPocketBudget budget
            join fetch budget.pocket pocket
            where budget.monthlyBudget.monthlyBudgetId = :monthlyBudgetId
              and pocket.user.userId = :userId
            """)
    List<MonthlyPocketBudget> findAllCurrentForUpdate(
            @Param("monthlyBudgetId") Long monthlyBudgetId,
            @Param("userId") Long userId
    );
}