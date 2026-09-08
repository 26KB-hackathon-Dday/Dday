package com.dday.domain.mockmydata.service;

import com.dday.domain.mockmydata.dto.response.MockAccountTransactionListResponse;
import com.dday.domain.mockmydata.entity.*;
import com.dday.domain.mockmydata.repository.*;
import com.dday.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MockMydataServiceTest {

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
    private MockMydataService mockMydataService;

    @Test
    void 계좌_목록은_활성_계좌의_외부_식별자만_응답한다() {
        MockMydataAccount account = MockMydataAccount.builder()
                .externalAccountId("ACC-001")
                .orgCode("KB")
                .accountNum("123-456")
                .accountName("주거래 통장")
                .productName("입출금 통장")
                .accountType(MockAccountType.DEPOSIT)
                .balance(500_000L)
                .availableBalance(450_000L)
                .build();
        given(userRepository.findByServiceUserId(1L))
                .willReturn(Optional.of(MockMydataUser.builder().serviceUserId(1L).name("사용자").build()));
        given(accountRepository.findAllByMockUserServiceUserIdAndActiveTrueOrderByMockAccountId(1L))
                .willReturn(List.of(account));

        var result = mockMydataService.findAccounts(1L);

        assertThat(result.getAccounts()).singleElement().satisfies(response -> {
            assertThat(response.getAccountId()).isEqualTo("ACC-001");
            assertThat(response.getBalance()).isEqualTo(500_000L);
        });
    }

    @Test
    void 존재하지_않는_Mock_사용자는_404_도메인_예외로_처리한다() {
        given(userRepository.findByServiceUserId(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> mockMydataService.findAccounts(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mock MyData 사용자를 찾을 수 없습니다.");

        verifyNoInteractions(accountRepository);
    }

    @Test
    void 계좌_거래는_외부_ID와_취소_원거래_ID를_유지한다() {
        LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 9, 1, 0, 0);
        MockMydataAccount account = MockMydataAccount.builder()
                .externalAccountId("ACC-001")
                .orgCode("KB")
                .accountNum("123-456")
                .accountType(MockAccountType.DEPOSIT)
                .build();
        ReflectionTestUtils.setField(account, "mockAccountId", 10L);
        MockMydataAccountTransaction transaction = MockMydataAccountTransaction.builder()
                .mockAccount(account)
                .externalTransactionId("TX-CANCEL-001")
                .transactionAt(from.plusDays(1))
                .transactionType(MockTransactionType.EXPENSE)
                .amount(10_000L)
                .originalTransactionId("TX-ORIGINAL-001")
                .build();
        transaction.markStatus(MockTransactionStatus.CANCELED, "TX-ORIGINAL-001");
        given(accountRepository.findByExternalAccountIdAndActiveTrue("ACC-001"))
                .willReturn(Optional.of(account));
        given(accountTransactionRepository.findInPeriod(10L, from, to))
                .willReturn(List.of(transaction));

        MockAccountTransactionListResponse result =
                mockMydataService.findAccountTransactions("ACC-001", from, to);

        assertThat(result.getTransactions()).singleElement().satisfies(response -> {
            assertThat(response.getTransactionId()).isEqualTo("TX-CANCEL-001");
            assertThat(response.getStatus()).isEqualTo(MockTransactionStatus.CANCELED);
            assertThat(response.getOriginalTransactionId()).isEqualTo("TX-ORIGINAL-001");
        });
    }

    @Test
    void 종료가_시작과_같거나_빠르면_거래를_조회하지_않는다() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);

        assertThatThrownBy(() ->
                mockMydataService.findCardTransactions("CARD-001", from, from))
                .isInstanceOf(BusinessException.class)
                .hasMessage("거래 조회 기간이 올바르지 않습니다.");

        verifyNoInteractions(cardRepository, cardTransactionRepository);
    }
}
