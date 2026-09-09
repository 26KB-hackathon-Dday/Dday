package com.dday.domain.mockmydata.service;

import com.dday.domain.mockmydata.entity.MockCardType;
import com.dday.domain.mockmydata.entity.MockMydataCard;
import com.dday.domain.mockmydata.entity.MockMydataUser;
import com.dday.domain.mockmydata.repository.MockMydataAccountRepository;
import com.dday.domain.mockmydata.repository.MockMydataAccountTransactionRepository;
import com.dday.domain.mockmydata.repository.MockMydataCardRepository;
import com.dday.domain.mockmydata.repository.MockMydataCardTransactionRepository;
import com.dday.domain.mockmydata.repository.MockMydataUserRepository;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * 복제가 필드를 빠뜨리지 않는지 지킨다.
 *
 * <p>카드에 한도를 추가했을 때 실제로 여기서 누락됐다 — 목데이터에는 한도가 있는데 복제된
 * 회원만 한도가 비어 이용률 화면이 통째로 빈 채로 나왔다. 필드가 늘 때마다 반복될 수 있는
 * 실수라 테스트로 못 박는다.
 */
@ExtendWith(MockitoExtension.class)
class MockMydataProvisionerTest {

    private static final Long TEMPLATE_USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

    @Mock
    private UserRepository serviceUserRepository;
    @Mock
    private MockMydataUserRepository userRepository;
    @Mock
    private MockMydataAccountRepository accountRepository;
    @Mock
    private MockMydataCardRepository cardRepository;
    @Mock
    private MockMydataAccountTransactionRepository accountTransactionRepository;
    @Mock
    private MockMydataCardTransactionRepository cardTransactionRepository;

    @InjectMocks
    private MockMydataProvisioner provisioner;

    @Captor
    private ArgumentCaptor<MockMydataCard> savedCard;

    @Test
    void 카드를_복제할_때_한도를_함께_옮긴다() {
        User templateUser = User.builder().build();
        ReflectionTestUtils.setField(templateUser, "userId", TEMPLATE_USER_ID);
        given(serviceUserRepository.findByEmail(MockMydataProvisioner.TEMPLATE_EMAIL))
                .willReturn(Optional.of(templateUser));

        MockMydataUser template = MockMydataUser.builder()
                .serviceUserId(TEMPLATE_USER_ID)
                .name("원본")
                .build();
        given(userRepository.findByServiceUserId(TARGET_USER_ID)).willReturn(Optional.empty());
        given(userRepository.findByServiceUserId(TEMPLATE_USER_ID)).willReturn(Optional.of(template));
        given(userRepository.save(any(MockMydataUser.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(accountRepository.findAllByMockUserServiceUserIdAndActiveTrueOrderByMockAccountId(
                TEMPLATE_USER_ID)).willReturn(List.of());
        given(cardRepository.findAllByMockUserServiceUserIdAndActiveTrueOrderByMockCardId(
                TEMPLATE_USER_ID)).willReturn(List.of(MockMydataCard.builder()
                .mockUser(template)
                .externalCardId("SH-CARD-0001")
                .orgCode("0306")
                .cardName("신한카드 Deep Dream")
                .cardType(MockCardType.CREDIT)
                .creditLimit(3_000_000L)
                .build()));
        given(cardRepository.save(any(MockMydataCard.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        boolean provisioned = provisioner.provision(TARGET_USER_ID, "복제본");

        assertThat(provisioned).isTrue();
        then(cardRepository).should().save(savedCard.capture());
        assertThat(savedCard.getValue().getCreditLimit()).isEqualTo(3_000_000L);
        assertThat(savedCard.getValue().getCardType()).isEqualTo(MockCardType.CREDIT);
    }
}
