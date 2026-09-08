package com.dday.domain.budgetadjustment.repository;

import com.dday.domain.mydata.entity.FinancialTransaction;
import com.dday.domain.mydata.entity.TransactionStatus;
import com.dday.domain.mydata.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BudgetAdjustmentTransactionRepository
        extends JpaRepository<FinancialTransaction, Long> {

    @Query("""
            select coalesce(sum(t.amount), 0)
            from FinancialTransaction t
            left join t.account a
            left join t.card c
            join t.pocket p
            where (a.user.userId = :userId or c.user.userId = :userId)
              and p.pocketId = :pocketId
              and t.transactionAt >= :from
              and t.transactionAt < :to
              and t.transactionType = :transactionType
              and t.transactionStatus = :transactionStatus
            """)
    Long sumSpentAmount(
            @Param("userId") Long userId,
            @Param("pocketId") Long pocketId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("transactionType") TransactionType transactionType,
            @Param("transactionStatus") TransactionStatus transactionStatus
    );
}