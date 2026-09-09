package com.dday.domain.credit.service;

import com.dday.domain.credit.repository.CreditScoreRepository;
import com.dday.domain.credit.repository.NonFinancialPaymentRepository;
import com.dday.domain.mockmydata.service.MockMydataProvisioner;
import com.dday.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 새로 가입한 회원에게 <b>신용관리 데모 데이터를 붙여준다.</b>
 *
 * <p>신용관리 화면 여섯 조각 중 넷은 마이데이터 연동이 알아서 채운다 —
 * 보유 카드({@code user_card})와 대출 목록({@code user_account})은
 * {@link MockMydataProvisioner}가 복제한 목데이터를 동기화가 옮겨온 결과다.
 * 나머지 둘, <b>신용점수 이력과 비금융 납부 이력(통신요금·건강보험료·국민연금)은
 * 어디서도 만들어지지 않아</b> 신규 가입자에게는 빈 화면이 떴다. 이 클래스가 그 둘을 채운다.
 *
 * <p><b>코드에 점수·금액을 다시 적지 않고 DB의 원본을 읽어 복제하는 이유</b>는
 * {@link MockMydataProvisioner}와 같다. 하드코딩하면 {@code data.sql}의 시드와 두 벌이 되어
 * 한쪽만 고쳤을 때 조용히 어긋난다. 데모 데이터를 바꾸고 싶으면 {@code data.sql} 한 곳만 고친다.
 *
 * <p>{@code NonFinancialPaymentService.sync}가 이미 데모 이력을 <i>만들어</i> 내지만 그걸 쓰지
 * 않는다. 그쪽은 '오늘'을 기준으로 12개월치를 생성해서, 시드가 손으로 맞춰 둔 개월 수(6개월)와
 * 연체 위치(2026-03 통신요금)가 달라진다. 요구는 "시드 그대로"라 생성이 아니라 복제여야 한다.
 *
 * <p><b>⚠️ 해커톤 시연 전용이다.</b> 실제 신용평가사·기관이 붙으면 이 클래스와 호출부를 지운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditDemoProvisioner {

    private final UserRepository userRepository;
    private final CreditScoreRepository creditScoreRepository;
    private final NonFinancialPaymentRepository paymentRepository;

    /**
     * 이 회원의 신용관리 데모 데이터를 만든다. <b>여러 번 불러도 안전하다</b> —
     * 가입 때 한 번, 마이데이터 연동 때 한 번 불리므로 두 배가 되면 안 된다.
     *
     * <p>원본이 없으면(시드를 안 넣은 DB) 조용히 건너뛴다. 데모 데이터가 없다고 해서
     * 회원가입이나 연동 자체가 실패하면 안 되기 때문이다 —
     * {@link MockMydataProvisioner#provision}과 같은 방침이다.
     *
     * @return 실제로 한 건이라도 넣었으면 true, 건너뛰었으면 false
     */
    @Transactional
    public boolean provision(Long userId) {
        if (userId == null) {
            return false;
        }

        /*
         * 원본은 목데이터와 같은 데모 계정이다. 상수를 여기서 새로 선언하지 않고
         * MockMydataProvisioner 것을 그대로 쓴다 — 두 벌이 되면 한쪽만 바뀌었을 때
         * 카드·대출은 A 계정에서, 신용점수는 B 계정에서 오는 화면이 된다.
         */
        Long templateUserId = userRepository.findByEmail(MockMydataProvisioner.TEMPLATE_EMAIL)
                .map(user -> user.getUserId())
                .orElse(null);
        if (templateUserId == null) {
            log.warn("신용 데모 원본({})이 없어 복제를 건너뛴다. data.sql 시드를 확인할 것.",
                    MockMydataProvisioner.TEMPLATE_EMAIL);
            return false;
        }
        if (templateUserId.equals(userId)) {
            return false;   // 원본 본인이면 복제할 게 없다
        }

        // 신용점수는 유일 제약이 없어 SQL만으로는 멱등하지 않다. 있으면 아예 넣지 않는다.
        int scores = creditScoreRepository.existsByUserUserId(userId)
                ? 0
                : creditScoreRepository.copyHistoryFrom(userId, templateUserId);

        // 납부 이력은 (user_id, payment_type, billing_month) 유일 제약 + insert ignore라
        // 그냥 다시 불러도 이미 있는 달이 걸러진다.
        int payments = paymentRepository.copyHistoryFrom(userId, templateUserId);

        if (scores + payments == 0) {
            return false;
        }

        log.info("신용 데모 데이터를 복제했다: userId={}, 신용점수={}건, 납부이력={}건",
                userId, scores, payments);
        return true;
    }
}
