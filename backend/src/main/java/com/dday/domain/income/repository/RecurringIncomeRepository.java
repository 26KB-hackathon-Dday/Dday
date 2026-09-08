package com.dday.domain.income.repository;

import com.dday.domain.income.entity.RecurringIncome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecurringIncomeRepository extends JpaRepository<RecurringIncome, Long> {

    /** 회원의 정기수입 전체. 등록 순서대로 보여줘야 화면에서 항목이 튀지 않는다. */
    List<RecurringIncome> findAllByUserUserIdOrderByRecurringIncomeIdAsc(Long userId);

    /**
     * <b>id만으로 찾지 않고 소유자를 함께 건다.</b> 삭제·수정에서 이 메서드를 쓰면
     * 남의 수입을 건드리는 요청이 조회 단계에서 빈 값이 되어 애초에 진행되지 않는다.
     */
    Optional<RecurringIncome> findByRecurringIncomeIdAndUserUserId(Long recurringIncomeId, Long userId);
}
