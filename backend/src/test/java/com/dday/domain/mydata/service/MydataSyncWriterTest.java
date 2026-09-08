package com.dday.domain.mydata.service;

import com.dday.domain.mydata.client.dto.MydataAccountTransactionData;
import com.dday.domain.mydata.entity.*;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.mydata.repository.UserAccountRepository;
import com.dday.domain.mydata.repository.UserCardRepository;
import com.dday.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MydataSyncWriterTest {

    @Mock private UserRepository userRepository;
    @Mock private UserAccountRepository accountRepository;
    @Mock private UserCardRepository cardRepository;
    @Mock private FinancialTransactionRepository transactionRepository;
    @InjectMocks private MydataSyncWriter writer;

    @Test
    void 신규_거래를_저장하고_취소_거래를_원거래에_연결한다() {
        UserAccount account = org.mockito.Mockito.mock(UserAccount.class);
        given(account.getAccountId()).willReturn(10L);
        given(account.getAccountNum()).willReturn("111-222");
        given(transactionRepository.findAllByAccountAccountIdAndSourceTransactionIdIn(eq(10L), anyList()))
                .willReturn(List.of());
        given(transactionRepository.save(any(FinancialTransaction.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        MydataAccountTransactionData original = transaction("TX-1", null, "EXPENSE",
                TransactionStatus.NORMAL, null);
        MydataAccountTransactionData canceled = transaction("TX-2", "TX-1", "EXPENSE",
                TransactionStatus.CANCELED, null);

        MydataWriteResult result = writer.saveTransactions(
                List.of(new MydataTransactionBatch.AccountBatch(account, List.of(canceled, original))),
                List.of(), LocalDateTime.now());

        assertThat(result.getInserted()).isEqualTo(2);
        assertThat(result.getLinkedCancellation()).isEqualTo(1);
    }

    @Test
    void 이미_저장한_외부_거래_ID는_다시_저장하지_않는다() {
        UserAccount account = org.mockito.Mockito.mock(UserAccount.class);
        given(account.getAccountId()).willReturn(10L);
        given(account.getAccountNum()).willReturn("111-222");
        FinancialTransaction existing = FinancialTransaction.builder()
                .sourceType(TransactionSourceType.ACCOUNT).account(account)
                .sourceTransactionId("TX-1").transactionAt(LocalDateTime.now())
                .syncedAt(LocalDateTime.now()).transactionType(TransactionType.EXPENSE)
                .amount(1_000L).build();
        given(transactionRepository.findAllByAccountAccountIdAndSourceTransactionIdIn(eq(10L), anyList()))
                .willReturn(List.of(existing));

        MydataWriteResult result = writer.saveTransactions(
                List.of(new MydataTransactionBatch.AccountBatch(account,
                        List.of(transaction("TX-1", null, "EXPENSE", TransactionStatus.NORMAL, null)))),
                List.of(), LocalDateTime.now());

        assertThat(result.getInserted()).isZero();
        assertThat(result.getSkipped()).isEqualTo(1);
    }

    @Test
    void 상대_계좌가_내_계좌이면_본인_이체로_저장한다() {
        UserAccount source = org.mockito.Mockito.mock(UserAccount.class);
        UserAccount counterparty = org.mockito.Mockito.mock(UserAccount.class);
        given(source.getAccountId()).willReturn(10L);
        given(source.getAccountNum()).willReturn("111");
        given(counterparty.getAccountNum()).willReturn("222");
        given(transactionRepository.findAllByAccountAccountIdAndSourceTransactionIdIn(eq(10L), anyList()))
                .willReturn(List.of());
        given(transactionRepository.save(any(FinancialTransaction.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        writer.saveTransactions(List.of(
                new MydataTransactionBatch.AccountBatch(source,
                        List.of(transaction("TX-1", null, "TRANSFER", TransactionStatus.NORMAL, "222"))),
                new MydataTransactionBatch.AccountBatch(counterparty, List.of())),
                List.of(), LocalDateTime.now());

        org.mockito.ArgumentCaptor<FinancialTransaction> captor =
                org.mockito.ArgumentCaptor.forClass(FinancialTransaction.class);
        org.mockito.Mockito.verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getTransactionType()).isEqualTo(TransactionType.SELF_TRANSFER);
        assertThat(captor.getValue().getCounterpartyAccount()).isSameAs(counterparty);
    }

    private MydataAccountTransactionData transaction(String id, String originalId, String type,
                                                       TransactionStatus status, String counterparty) {
        return MydataAccountTransactionData.builder()
                .transactionId(id)
                .transactionAt(LocalDateTime.of(2026, 9, 1, 12, 0))
                .transactionType(type)
                .amount(10_000L)
                .status(status)
                .originalTransactionId(originalId)
                .counterpartyAccountNum(counterparty)
                .build();
    }
}
