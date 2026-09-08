package com.dday.domain.budgetadjustment.repository;

import com.dday.domain.budget.entity.BudgetChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetAdjustmentHistoryRepository
        extends JpaRepository<BudgetChangeHistory, Long> {
}