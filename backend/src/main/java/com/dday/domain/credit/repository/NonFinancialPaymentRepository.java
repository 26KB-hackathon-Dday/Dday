package com.dday.domain.credit.repository;

import com.dday.domain.credit.entity.NonFinancialPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 비금융 납부 이력 조회.
 *
 * <p>한 회원의 이력은 유형 셋 × 최근 몇 달이라 많아야 수십 건이다. 정렬·그룹핑을 쿼리로
 * 나누지 않고 통째로 읽어 서비스에서 묶는다 — 화면이 유형별 요약과 목록을 같이 쓰기 때문에
 * 어차피 전부 필요하다.
 */
public interface NonFinancialPaymentRepository extends JpaRepository<NonFinancialPayment, Long> {

    List<NonFinancialPayment> findAllByUserUserId(Long userId);
}
