package com.dday.domain.mockmydata.repository;

import com.dday.domain.mockmydata.entity.MockMydataCardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MockMydataCardTransactionRepository
        extends JpaRepository<MockMydataCardTransaction, Long> {

    @Query("""
            select transaction
            from MockMydataCardTransaction transaction
            where transaction.mockCard.mockCardId = :mockCardId
              and transaction.transactionAt >= :from
              and transaction.transactionAt < :to
            order by transaction.transactionAt desc, transaction.mockCardTransactionId desc
            """)
    List<MockMydataCardTransaction> findInPeriod(
            @Param("mockCardId") Long mockCardId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
