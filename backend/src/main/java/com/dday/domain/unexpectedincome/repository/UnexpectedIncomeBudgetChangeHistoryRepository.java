package com.dday.domain.unexpectedincome.repository;

import com.dday.domain.budget.entity.BudgetChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnexpectedIncomeBudgetChangeHistoryRepository
        extends JpaRepository<BudgetChangeHistory, Long> {
}