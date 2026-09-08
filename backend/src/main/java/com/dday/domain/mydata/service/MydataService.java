package com.dday.domain.mydata.service;

import com.dday.domain.mydata.client.MydataClient;
import com.dday.domain.mydata.client.dto.*;
import com.dday.domain.mydata.dto.MydataErrorCode;
import com.dday.domain.mydata.dto.response.MydataSyncResponse;
import com.dday.global.exception.BusinessException;
import com.dday.domain.pocket.service.TransactionClassificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 계좌·카드 목록 수집부터 거래 저장까지 MyData 동기화 순서를 조정한다.
 *
 * <p>이 클래스에는 {@code @Transactional}을 붙이지 않는다. 외부 공급자 호출 사이에 DB
 * 트랜잭션을 열어 두면 느린 네트워크 때문에 커넥션이 오래 점유된다. 실제 쓰기 원자성은
 * {@link MydataSyncWriter}의 짧은 트랜잭션이 보장한다.
 */
@Service
@RequiredArgsConstructor
public class MydataService {

    private final MydataClient mydataClient;
    private final MydataSyncWriter syncWriter;
    private final TransactionClassificationService classificationService;

    /**
     * 사용자의 금융 원천을 갱신하고 요청 기간의 모든 계좌·카드 거래를 동기화한다.
     * 기간을 생략하면 현재 월을 포함한 최근 3개월을 사용한다.
     */
    public MydataSyncResponse sync(Long userId, LocalDateTime requestedFrom,
                                   LocalDateTime requestedTo) {
        LocalDateTime syncedAt = LocalDateTime.now();
        SyncPeriod period = period(requestedFrom, requestedTo, syncedAt);
        syncWriter.validateUser(userId, syncedAt);

        // 외부 조회를 먼저 모두 끝낸다. 중간 호출이 실패하면 서비스 DB는 전혀 변경되지 않는다.
        List<MydataAccountData> accounts = mydataClient.getAccounts(userId);
        List<MydataCardData> cards = mydataClient.getCards(userId);
        Map<String, List<MydataAccountTransactionData>> accountTransactions = new LinkedHashMap<>();
        int accountTransactionCount = 0;
        for (MydataAccountData source : accounts) {
            List<MydataAccountTransactionData> transactions = mydataClient.getAccountTransactions(
                    source.getExternalAccountId(), period.from, period.to);
            accountTransactionCount += transactions.size();
            accountTransactions.put(source.getExternalAccountId(), transactions);
        }

        Map<String, List<MydataCardTransactionData>> cardTransactions = new LinkedHashMap<>();
        int cardTransactionCount = 0;
        for (MydataCardData source : cards) {
            List<MydataCardTransactionData> transactions = mydataClient.getCardTransactions(
                    source.getExternalCardId(), period.from, period.to);
            cardTransactionCount += transactions.size();
            cardTransactions.put(source.getExternalCardId(), transactions);
        }

        MydataSourceSnapshot sources;
        try {
            sources = syncWriter.syncSources(userId, accounts, cards, syncedAt);
        } catch (DataAccessException exception) {
            // DB 기술 예외를 그대로 노출하지 않고 MyData 도메인의 일관된 실패 코드로 바꾼다.
            throw new BusinessException(MydataErrorCode.MYDATA_SYNC_FAILURE);
        }
        List<MydataTransactionBatch.AccountBatch> accountBatches = new ArrayList<>();
        accountTransactions.forEach((externalId, transactions) ->
                accountBatches.add(new MydataTransactionBatch.AccountBatch(
                        sources.getAccountsByExternalId().get(externalId), transactions)));
        List<MydataTransactionBatch.CardBatch> cardBatches = new ArrayList<>();
        cardTransactions.forEach((externalId, transactions) ->
            cardBatches.add(new MydataTransactionBatch.CardBatch(
                    sources.getCardsByExternalId().get(externalId), transactions)));

        MydataWriteResult result;
        try {
            result = syncWriter.saveTransactions(accountBatches, cardBatches, syncedAt);
        } catch (DataAccessException exception) {
            throw new BusinessException(MydataErrorCode.MYDATA_SYNC_FAILURE);
        }
        // 새 거래 저장이 끝난 뒤 별도 분류 트랜잭션을 실행한다. 실패해도 원본 거래는 남아 재시도할 수 있다.
        classificationService.classifyUnclassified(userId);
        return MydataSyncResponse.builder()
                .accountCount(accounts.size())
                .cardCount(cards.size())
                .accountTransactionCount(accountTransactionCount)
                .cardTransactionCount(cardTransactionCount)
                .insertedTransactionCount(result.getInserted())
                .skippedTransactionCount(result.getSkipped())
                .linkedCancellationCount(result.getLinkedCancellation())
                .syncedAt(syncedAt)
                .build();
    }

    private SyncPeriod period(LocalDateTime from, LocalDateTime to, LocalDateTime now) {
        if (from == null && to == null) {
            YearMonth current = YearMonth.from(now);
            return new SyncPeriod(
                    current.minusMonths(2).atDay(1).atStartOfDay(),
                    current.plusMonths(1).atDay(1).atStartOfDay());
        }
        // 한쪽만 생략하면 호출자의 의도를 추측할 수 없으므로 명시적인 400으로 처리한다.
        if (from == null || to == null || !from.isBefore(to)) {
            throw new BusinessException(MydataErrorCode.INVALID_SYNC_PERIOD);
        }
        return new SyncPeriod(from, to);
    }

    /** 종료 시각을 포함하지 않는 [from, to) 조회 범위다. */
    private static class SyncPeriod {
        private final LocalDateTime from;
        private final LocalDateTime to;

        private SyncPeriod(LocalDateTime from, LocalDateTime to) {
            this.from = from;
            this.to = to;
        }
    }
}
