package com.dday.domain.mydata.client;

import com.dday.domain.mydata.client.dto.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyData 공급자를 서비스 로직에서 분리하는 경계다.
 * Mock 대신 실제 금융 API를 붙일 때 서비스는 유지하고 이 구현체만 교체한다.
 */
public interface MydataClient {
    List<MydataAccountData> getAccounts(Long userId);
    List<MydataCardData> getCards(Long userId);
    List<MydataAccountTransactionData> getAccountTransactions(
            String externalAccountId, LocalDateTime from, LocalDateTime to);
    List<MydataCardTransactionData> getCardTransactions(
            String externalCardId, LocalDateTime from, LocalDateTime to);
}
