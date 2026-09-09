package com.dday.domain.unexpectedincome.repository;

import com.dday.domain.budget.entity.BudgetChangeDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnexpectedIncomeBudgetChangeDetailRepository
        extends JpaRepository<BudgetChangeDetail, Long> {
}