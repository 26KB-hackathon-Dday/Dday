package com.dday.domain.unexpectedincome.repository;

import com.dday.domain.income.entity.RecurringIncome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnexpectedIncomeRecurringIncomeRepository
        extends JpaRepository<RecurringIncome, Long> {

    List<RecurringIncome>
    findAllByUserUserIdOrderByRecurringIncomeIdAsc(
            Long userId
    );
}