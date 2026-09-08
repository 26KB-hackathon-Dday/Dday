package com.dday.domain.budget.repository;

import com.dday.domain.budget.entity.MonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {

    /** 사용자 소유권과 기준 월을 함께 제한해 한 달의 최상위 예산을 조회한다. */
    Optional<MonthlyBudget> findByUserUserIdAndBudgetMonth(Long userId, LocalDate budgetMonth);
}
