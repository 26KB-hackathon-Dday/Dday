package com.dday.domain.budgetadjustment.repository;

import com.dday.domain.budget.entity.MonthlyPocketBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BudgetAdjustmentMonthlyPocketBudgetRepository
        extends JpaRepository<MonthlyPocketBudget, Long> {

    @Query("""
            select mpb
            from MonthlyPocketBudget mpb
            join fetch mpb.pocket p
            where mpb.monthlyBudget.monthlyBudgetId = :monthlyBudgetId
            """)
    List<MonthlyPocketBudget> findAllByMonthlyBudgetId(
            @Param("monthlyBudgetId") Long monthlyBudgetId
    );
}