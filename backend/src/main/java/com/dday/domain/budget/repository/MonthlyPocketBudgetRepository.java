package com.dday.domain.budget.repository;

import com.dday.domain.budget.entity.MonthlyPocketBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MonthlyPocketBudgetRepository extends JpaRepository<MonthlyPocketBudget, Long> {

    /**
     * 한 월 예산에 속한 포켓별 배정액을 조회한다.
     *
     * <p>월 예산 ID만으로 조회하지 않고 포켓 소유자도 다시 확인해 다른 사용자의 포켓이 섞이는
     * 것을 막는다. 응답 구성에서 포켓 유형과 이름을 바로 사용하므로 포켓을 fetch join한다.
     */
    @Query("""
            select budget
            from MonthlyPocketBudget budget
            join fetch budget.pocket pocket
            where budget.monthlyBudget.monthlyBudgetId = :monthlyBudgetId
              and pocket.user.userId = :userId
            """)
    List<MonthlyPocketBudget> findAllByMonthlyBudgetAndUser(
            @Param("monthlyBudgetId") Long monthlyBudgetId,
            @Param("userId") Long userId);
}
