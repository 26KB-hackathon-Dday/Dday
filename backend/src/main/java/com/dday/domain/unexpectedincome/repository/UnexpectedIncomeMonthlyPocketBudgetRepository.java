package com.dday.domain.unexpectedincome.repository;

import com.dday.domain.budget.entity.MonthlyPocketBudget;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UnexpectedIncomeMonthlyPocketBudgetRepository
        extends JpaRepository<MonthlyPocketBudget, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select mpb
            from MonthlyPocketBudget mpb
            join fetch mpb.pocket p
            where mpb.monthlyBudget.monthlyBudgetId = :monthlyBudgetId
            order by mpb.monthlyPocketBudgetId asc
            """)
    List<MonthlyPocketBudget> findAllForUpdate(
            @Param("monthlyBudgetId") Long monthlyBudgetId
    );
}