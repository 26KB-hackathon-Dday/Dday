package com.dday.domain.mydata.service;

import com.dday.domain.mydata.client.MydataClient;
import com.dday.domain.mydata.client.dto.*;
import com.dday.domain.mydata.dto.MydataErrorCode;
import com.dday.domain.mydata.dto.response.MydataSyncResponse;
import com.dday.domain.mydata.entity.UserAccount;
import com.dday.domain.mydata.entity.UserCard;
import com.dday.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MydataServiceTest {

    @Mock private MydataClient mydataClient;
    @Mock private MydataSyncWriter syncWriter;
    @InjectMocks private MydataService mydataService;

    @Test
    void 계좌와_카드_거래를_수집하고_저장_결과를_응답한다() {
        MydataAccountData accountData = MydataAccountData.builder()
                .externalAccountId("ACC-1").build();
        MydataCardData cardData = MydataCardData.builder().externalCardId("CARD-1").build();
        UserAccount account = org.mockito.Mockito.mock(UserAccount.class);
        UserCard card = org.mockito.Mockito.mock(UserCard.class);
        given(mydataClient.getAccounts(1L)).willReturn(List.of(accountData));
        given(mydataClient.getCards(1L)).willReturn(List.of(cardData));
        given(syncWriter.syncSources(eq(1L), anyList(), anyList(), any()))
                .willReturn(new MydataSourceSnapshot(Map.of("ACC-1", account), Map.of("CARD-1", card)));
        given(mydataClient.getAccountTransactions(eq("ACC-1"), any(), any()))
                .willReturn(List.of(MydataAccountTransactionData.builder().transactionId("AT-1").build()));
        given(mydataClient.getCardTransactions(eq("CARD-1"), any(), any()))
                .willReturn(List.of(MydataCardTransactionData.builder().transactionId("CT-1").build()));
        MydataWriteResult writeResult = new MydataWriteResult();
        writeResult.addInserted();
        writeResult.addSkipped();
        given(syncWriter.saveTransactions(anyList(), anyList(), any())).willReturn(writeResult);

        MydataSyncResponse response = mydataService.sync(1L, null, null);

        assertThat(response.getAccountCount()).isEqualTo(1);
        assertThat(response.getCardCount()).isEqualTo(1);
        assertThat(response.getInsertedTransactionCount()).isEqualTo(1);
        assertThat(response.getSkippedTransactionCount()).isEqualTo(1);
        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mydataClient).getAccountTransactions(eq("ACC-1"), from.capture(), to.capture());
        assertThat(from.getValue().getDayOfMonth()).isEqualTo(1);
        assertThat(to.getValue().getDayOfMonth()).isEqualTo(1);
        assertThat(java.time.temporal.ChronoUnit.MONTHS.between(
                java.time.YearMonth.from(from.getValue()), java.time.YearMonth.from(to.getValue())))
                .isEqualTo(3);
    }

    @Test
    void 동기화_기간은_둘_중_하나만_보낼_수_없다() {
        assertThatThrownBy(() -> mydataService.sync(1L, LocalDateTime.now(), null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(MydataErrorCode.INVALID_SYNC_PERIOD));
    }
}
