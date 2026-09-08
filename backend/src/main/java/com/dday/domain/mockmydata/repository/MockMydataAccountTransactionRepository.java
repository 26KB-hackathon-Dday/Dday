package com.dday.domain.mockmydata.repository;

import com.dday.domain.mockmydata.entity.MockMydataAccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MockMydataAccountTransactionRepository
        extends JpaRepository<MockMydataAccountTransaction, Long> {

    @Query("""
            select transaction
            from MockMydataAccountTransaction transaction
            where transaction.mockAccount.mockAccountId = :mockAccountId
              and transaction.transactionAt >= :from
              and transaction.transactionAt < :to
            order by transaction.transactionAt desc, transaction.mockTransactionId desc
            """)
    List<MockMydataAccountTransaction> findInPeriod(
            @Param("mockAccountId") Long mockAccountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
