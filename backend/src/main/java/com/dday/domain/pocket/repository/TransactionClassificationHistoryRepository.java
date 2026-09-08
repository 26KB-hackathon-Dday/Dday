package com.dday.domain.pocket.repository;

import com.dday.domain.pocket.entity.TransactionClassificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/** 거래 분류 변경의 이전 값과 변경 주체를 보존한다. */
public interface TransactionClassificationHistoryRepository
        extends JpaRepository<TransactionClassificationHistory, Long> {
}
