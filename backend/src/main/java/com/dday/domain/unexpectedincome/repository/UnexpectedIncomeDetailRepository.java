package com.dday.domain.unexpectedincome.repository;

import com.dday.domain.budget.entity.BudgetChangeDetail;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 예상 밖 수입으로 변경된 각 포켓 금액의
 * 상세 이력을 저장한다.
 */
public interface UnexpectedIncomeDetailRepository
        extends JpaRepository<BudgetChangeDetail, Long> {
}