package com.dday.domain.credit.repository;

import com.dday.domain.credit.entity.NonFinancialPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 다른 회원의 납부 이력을 통째로 복제한다. 시연에서 누가 가입하든 같은 납부 기록이
     * 보이게 하려고 {@code CreditDemoProvisioner}가 부른다. 새로 넣은 건수를 돌려준다.
     *
     * <p><b>네이티브 SQL인 이유는 청구월·납부일을 원본 그대로 옮겨야 하기 때문이다.</b>
     * {@code NonFinancialPaymentService.sync}는 '오늘'을 기준으로 12개월치를 만들어 내는데,
     * 시드가 손으로 맞춰 둔 연체 위치(2026-03 통신요금)와 개월 수가 달라진다. 시연 화면을
     * 시드와 똑같이 맞추려면 만들어 내는 게 아니라 복제해야 한다.
     *
     * <p>{@code insert ignore}라 여러 번 불러도 안전하다 —
     * {@code uk_nonfinancial_payment_user_type_month}가 이미 있는 달을 걸러낸다.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert ignore into nonfinancial_payment (
                user_id, payment_type, institution_name, billing_month, amount,
                due_date, paid_date, status, created_at, updated_at)
            select :userId, t.payment_type, t.institution_name, t.billing_month, t.amount,
                   t.due_date, t.paid_date, t.status, t.created_at, t.updated_at
              from nonfinancial_payment t
             where t.user_id = :templateUserId
            """, nativeQuery = true)
    int copyHistoryFrom(@Param("userId") Long userId,
                        @Param("templateUserId") Long templateUserId);
}
