package com.dday.domain.unexpectedincome.repository;

import com.dday.domain.mydata.entity.FinancialTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UnexpectedIncomeTransactionRepository
        extends JpaRepository<FinancialTransaction, Long> {

    /**
     * 아직 사용자가 처리하지 않은 신규 입금을
     * 오래된 순서대로 모두 조회한다.
     */
    @Query("""
            select t
            from FinancialTransaction t
            left join fetch t.account a
            left join fetch t.card c
            where (a.user.userId = :userId or c.user.userId = :userId)
              and t.transactionType =
                  com.dday.domain.mydata.entity.TransactionType.INCOME
              and t.transactionStatus =
                  com.dday.domain.mydata.entity.TransactionStatus.NORMAL
              and t.newFundChecked = false
            order by t.transactionAt asc,
                     t.financialTransactionId asc
            """)
    List<FinancialTransaction> findPendingIncomes(
            @Param("userId") Long userId
    );

    /**
     * 실제 예산 반영/제외 처리할 때
     * 동시 수정 방지를 위해 잠금 조회한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from FinancialTransaction t
            left join fetch t.account a
            left join fetch t.card c
            where t.financialTransactionId = :transactionId
              and (a.user.userId = :userId or c.user.userId = :userId)
              and t.transactionType =
                  com.dday.domain.mydata.entity.TransactionType.INCOME
              and t.transactionStatus =
                  com.dday.domain.mydata.entity.TransactionStatus.NORMAL
            """)
    Optional<FinancialTransaction> findIncomeForUpdate(
            @Param("userId") Long userId,
            @Param("transactionId") Long transactionId
    );
}