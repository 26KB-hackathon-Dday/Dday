package com.dday.domain.credit.service;

import com.dday.domain.credit.repository.CreditScoreRepository;
import com.dday.domain.credit.repository.NonFinancialPaymentRepository;
import com.dday.domain.mockmydata.service.MockMydataProvisioner;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 데모 계정의 신용 이력을 신규 가입자에게 복제하는 규칙을 확인한다.
 *
 * <p>가입과 마이데이터 연동 양쪽에서 불리므로 <b>두 번 불려도 이력이 두 배가 되지 않는 것</b>이
 * 핵심이다. 신용점수는 유일 제약이 없어 SQL이 막아주지 못하고 이 클래스의 검사에 달려 있다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreditDemoProvisionerTest {

    private static final Long TEMPLATE_USER_ID = 9001L;
    private static final Long TARGET_USER_ID = 42L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private CreditScoreRepository creditScoreRepository;
    @Mock
    private NonFinancialPaymentRepository paymentRepository;

    @InjectMocks
    private CreditDemoProvisioner provisioner;

    private void givenTemplate() {
        User template = User.builder().email(MockMydataProvisioner.TEMPLATE_EMAIL).build();
        ReflectionTestUtils.setField(template, "userId", TEMPLATE_USER_ID);
        given(userRepository.findByEmail(MockMydataProvisioner.TEMPLATE_EMAIL))
                .willReturn(Optional.of(template));
    }

    @Test
    void 데모_계정의_신용점수와_납부이력을_복제한다() {
        givenTemplate();
        given(creditScoreRepository.existsByUserUserId(TARGET_USER_ID)).willReturn(false);
        given(creditScoreRepository.copyHistoryFrom(TARGET_USER_ID, TEMPLATE_USER_ID)).willReturn(6);
        given(paymentRepository.copyHistoryFrom(TARGET_USER_ID, TEMPLATE_USER_ID)).willReturn(18);

        boolean provisioned = provisioner.provision(TARGET_USER_ID);

        assertThat(provisioned).isTrue();
        verify(creditScoreRepository).copyHistoryFrom(TARGET_USER_ID, TEMPLATE_USER_ID);
        verify(paymentRepository).copyHistoryFrom(TARGET_USER_ID, TEMPLATE_USER_ID);
    }

    @Test
    void 이미_신용점수_이력이_있으면_다시_넣지_않는다() {
        givenTemplate();
        given(creditScoreRepository.existsByUserUserId(TARGET_USER_ID)).willReturn(true);
        given(paymentRepository.copyHistoryFrom(TARGET_USER_ID, TEMPLATE_USER_ID)).willReturn(0);

        boolean provisioned = provisioner.provision(TARGET_USER_ID);

        assertThat(provisioned).isFalse();
        verify(creditScoreRepository, never()).copyHistoryFrom(anyLong(), anyLong());
    }

    /** 시드를 안 넣은 DB에서도 가입·연동이 깨지면 안 된다. */
    @Test
    void 원본_계정이_없으면_조용히_건너뛴다() {
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());

        boolean provisioned = provisioner.provision(TARGET_USER_ID);

        assertThat(provisioned).isFalse();
        verify(creditScoreRepository, never()).copyHistoryFrom(anyLong(), anyLong());
        verify(paymentRepository, never()).copyHistoryFrom(anyLong(), anyLong());
    }

    /** 원본 본인이 다시 복제하면 자기 이력이 두 배가 된다. */
    @Test
    void 원본_계정_본인에게는_붙이지_않는다() {
        givenTemplate();

        boolean provisioned = provisioner.provision(TEMPLATE_USER_ID);

        assertThat(provisioned).isFalse();
        verify(creditScoreRepository, never()).copyHistoryFrom(anyLong(), anyLong());
        verify(paymentRepository, never()).copyHistoryFrom(anyLong(), anyLong());
    }
}
