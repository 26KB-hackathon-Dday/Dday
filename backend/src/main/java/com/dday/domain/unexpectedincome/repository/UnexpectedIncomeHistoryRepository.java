package com.dday.domain.unexpectedincome.repository;

import com.dday.domain.budget.entity.BudgetChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 예상 밖 수입으로 인한 예산 변경 이력 저장용 Repository.
 */
public interface UnexpectedIncomeHistoryRepository
        extends JpaRepository<BudgetChangeHistory, Long> {
}