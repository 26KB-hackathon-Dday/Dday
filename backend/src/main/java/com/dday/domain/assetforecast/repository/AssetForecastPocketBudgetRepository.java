package com.dday.domain.assetforecast.repository;

import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.pocket.entity.PocketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface AssetForecastPocketBudgetRepository
        extends JpaRepository<MonthlyPocketBudget, Long> {

    /**
     * 특정 사용자의 특정 월 미래자산 포켓 예산을 조회한다.
     *
     * monthly_budget -> monthly_pocket_budget -> pocket을 연결해서
     * FUTURE_ASSET 하나만 가져온다.
     */
    @Query("""
            select pocketBudget
            from MonthlyPocketBudget pocketBudget
            join fetch pocketBudget.pocket pocket
            join pocketBudget.monthlyBudget monthlyBudget
            where monthlyBudget.user.userId = :userId
              and monthlyBudget.budgetMonth = :budgetMonth
              and pocket.pocketType = :pocketType
            """)
    Optional<MonthlyPocketBudget> findByUserAndMonthAndPocketType(
            @Param("userId") Long userId,
            @Param("budgetMonth") LocalDate budgetMonth,
            @Param("pocketType") PocketType pocketType
    );
}