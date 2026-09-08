package com.dday.domain.budgetadjustment.repository;

import com.dday.domain.budget.entity.BudgetChangeDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetAdjustmentDetailRepository
        extends JpaRepository<BudgetChangeDetail, Long> {
}