package com.dday.domain.budgetadjustment.repository;

import com.dday.domain.budget.entity.BudgetStatus;
import com.dday.domain.budget.entity.MonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface BudgetAdjustmentMonthlyBudgetRepository
        extends JpaRepository<MonthlyBudget, Long> {

    Optional<MonthlyBudget> findByUserUserIdAndBudgetMonthAndBudgetStatus(
            Long userId,
            LocalDate budgetMonth,
            BudgetStatus budgetStatus
    );
}