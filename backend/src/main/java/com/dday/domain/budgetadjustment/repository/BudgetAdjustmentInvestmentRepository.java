package com.dday.domain.budgetadjustment.repository;

import com.dday.domain.asset.entity.ExecutionStatus;
import com.dday.domain.asset.entity.InvestmentActionType;
import com.dday.domain.asset.entity.InvestmentTransaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Set;

public interface BudgetAdjustmentInvestmentRepository
        extends JpaRepository<InvestmentTransaction, Long> {

    @Query("""
            select coalesce(sum(i.amount), 0)
            from InvestmentTransaction i
            where i.account.user.userId = :userId
              and i.executedAt >= :from
              and i.executedAt < :to
              and i.executionStatus = :executionStatus
              and i.actionType in :actions
            """)
    Long sumAmountByActions(
            @Param("userId")
            Long userId,

            @Param("from")
            LocalDateTime from,

            @Param("to")
            LocalDateTime to,

            @Param("executionStatus")
            ExecutionStatus executionStatus,

            @Param("actions")
            Set<InvestmentActionType> actions
    );
}