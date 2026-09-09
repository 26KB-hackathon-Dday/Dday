package com.dday.domain.credit.service;

import com.dday.domain.credit.dto.response.PaymentHistoryResponse;
import com.dday.domain.credit.dto.response.PaymentSyncResponse;
import com.dday.domain.credit.dto.response.PaymentTypeHistoryResponse;
import com.dday.domain.credit.entity.NonFinancialPayment;
import com.dday.domain.credit.entity.PaymentStatus;
import com.dday.domain.credit.entity.PaymentType;
import com.dday.domain.credit.repository.NonFinancialPaymentRepository;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NonFinancialPaymentServiceTest {

    private static final Long USER_ID = 1L;
    /** 기준월. 데모 이력은 이 달을 포함해 12개월을 거슬러 만든다. */
    private static final YearMonth BASE = YearMonth.of(2026, 9);

    @Mock
    private NonFinancialPaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NonFinancialPaymentService paymentService;

    @Captor
    private ArgumentCaptor<List<NonFinancialPayment>> savedCaptor;

    private final User user = User.builder().build();

    private void givenUser() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    }

    private NonFinancialPayment payment(PaymentType type, YearMonth month, PaymentStatus status) {
        LocalDate due = month.plusMonths(1).atDay(25);
        return NonFinancialPayment.builder()
                .user(user)
                .paymentType(type)
                .institutionName("SK텔레콤")
                .billingMonth(month.atDay(1))
                .amount(38_500L)
                .dueDate(due)
                .paidDate(status == PaymentStatus.UNPAID ? null
                        : (status == PaymentStatus.LATE ? due.plusDays(6) : due.minusDays(1)))
                .status(status)
                .build();
    }

    @Test
    void 데모_동기화는_유형마다_최근_12개월을_만든다() {
        givenUser();
        given(paymentRepository.findAllByUserUserId(USER_ID)).willReturn(List.of());

        PaymentSyncResponse result = paymentService.sync(USER_ID, BASE);

        then(paymentRepository).should().saveAll(savedCaptor.capture());
        // 통신요금·건강보험료·국민연금 세 유형 × 12개월
        assertThat(savedCaptor.getValue()).hasSize(36);
        assertThat(result.getCreatedCount()).isEqualTo(36);
        assertThat(result.getMonthsCovered()).isEqualTo(12);
    }

    @Test
    void 이미_있는_달은_다시_만들지_않는다() {
        givenUser();
        given(paymentRepository.findAllByUserUserId(USER_ID))
                .willReturn(List.of(payment(PaymentType.TELECOM, BASE, PaymentStatus.PAID)));

        PaymentSyncResponse result = paymentService.sync(USER_ID, BASE);

        then(paymentRepository).should().saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).hasSize(35);
        assertThat(result.getCreatedCount()).isEqualTo(35);
    }

    @Test
    void 조회는_유형별로_묶고_최신_청구월부터_보여준다() {
        given(paymentRepository.findAllByUserUserId(USER_ID)).willReturn(List.of(
                payment(PaymentType.TELECOM, BASE.minusMonths(1), PaymentStatus.PAID),
                payment(PaymentType.TELECOM, BASE, PaymentStatus.PAID),
                payment(PaymentType.NATIONAL_PENSION, BASE, PaymentStatus.PAID)));

        PaymentHistoryResponse result = paymentService.findHistory(USER_ID);

        // 유형 순서는 enum 선언 순서다 — 통신요금 → 건강보험료 → 국민연금.
        assertThat(result.getTypes()).extracting(PaymentTypeHistoryResponse::getPaymentType)
                .containsExactly(PaymentType.TELECOM, PaymentType.NATIONAL_PENSION);

        PaymentTypeHistoryResponse telecom = result.getTypes().get(0);
        assertThat(telecom.getLatestBillingMonth()).isEqualTo("2026-09");
        assertThat(telecom.getRecords()).extracting("billingMonth")
                .containsExactly("2026-09", "2026-08");
    }

    @Test
    void 업권_이름과_상태_라벨을_서버가_내려준다() {
        given(paymentRepository.findAllByUserUserId(USER_ID))
                .willReturn(List.of(payment(PaymentType.TELECOM, BASE, PaymentStatus.PAID)));

        PaymentHistoryResponse result = paymentService.findHistory(USER_ID);

        assertThat(result.getTypes().get(0).getLabel()).isEqualTo("통신요금");
        assertThat(result.getTypes().get(0).getRecords().get(0).getStatusLabel())
                .isEqualTo("정상 납부");
    }

    @Test
    void 연체가_있으면_연속_정상_납부가_거기서_끊긴다() {
        given(paymentRepository.findAllByUserUserId(USER_ID)).willReturn(List.of(
                payment(PaymentType.TELECOM, BASE, PaymentStatus.PAID),
                payment(PaymentType.TELECOM, BASE.minusMonths(1), PaymentStatus.PAID),
                payment(PaymentType.TELECOM, BASE.minusMonths(2), PaymentStatus.LATE),
                payment(PaymentType.TELECOM, BASE.minusMonths(3), PaymentStatus.PAID)));

        PaymentHistoryResponse result = paymentService.findHistory(USER_ID);

        PaymentTypeHistoryResponse telecom = result.getTypes().get(0);
        assertThat(telecom.getOnTimeStreak()).isEqualTo(2);
        assertThat(telecom.getLateCount()).isEqualTo(1);
        assertThat(result.getLateCount()).isEqualTo(1);
    }

    @Test
    void 이력이_없으면_빈_응답을_준다() {
        given(paymentRepository.findAllByUserUserId(USER_ID)).willReturn(List.of());

        PaymentHistoryResponse result = paymentService.findHistory(USER_ID);

        assertThat(result.getTypes()).isEmpty();
        assertThat(result.getMonthsCovered()).isZero();
        assertThat(result.getOnTimeCount()).isZero();
    }
}
