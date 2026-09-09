package com.dday.domain.mydata.service;

import com.dday.domain.mydata.client.dto.*;
import com.dday.domain.mydata.dto.MydataErrorCode;
import com.dday.domain.mydata.entity.*;
import com.dday.domain.mydata.repository.FinancialTransactionRepository;
import com.dday.domain.mydata.repository.UserAccountRepository;
import com.dday.domain.mydata.repository.UserCardRepository;
import com.dday.domain.user.entity.User;
import com.dday.domain.user.entity.UserStatus;
import com.dday.domain.user.dto.UserErrorCode;
import com.dday.domain.user.repository.UserRepository;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MyData 저장 작업의 짧은 트랜잭션 경계를 담당한다.
 *
 * <p>외부 조회는 {@link MydataService}가 트랜잭션 밖에서 수행한다. 네트워크 응답을 기다리는 동안
 * DB 연결을 점유하지 않고, 외부 호출이 모두 성공한 뒤 저장 단계만 원자적으로 처리하기 위함이다.
 */
@Service
@RequiredArgsConstructor
public class MydataSyncWriter {

    private final UserRepository userRepository;
    private final UserAccountRepository accountRepository;
    private final UserCardRepository cardRepository;
    private final FinancialTransactionRepository transactionRepository;

    /** 활성 회원과 MyData 동의 만료 시각을 동기화 시작 전에 확인한다. */
    @Transactional(readOnly = true)
    public void validateUser(Long userId, LocalDateTime now) {
        User user = userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (!user.hasValidMydataConsent(now)) {
            throw new BusinessException(MydataErrorCode.MYDATA_CONSENT_REQUIRED);
        }
    }

    /**
     * 계좌·카드를 외부 복합키로 upsert하고 이번 응답에서 사라진 원천은 비활성화한다.
     * 사용자 선택값은 엔티티의 sync 메서드가 보존한다.
     */
    @Transactional
    public MydataSourceSnapshot syncSources(Long userId, List<MydataAccountData> accountData,
                                            List<MydataCardData> cardData,
                                            LocalDateTime syncedAt) {
        User user = userRepository.findByUserIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        Set<String> accountKeys = accountData.stream().map(this::accountKey).collect(Collectors.toSet());
        accountRepository.findAllByUserUserIdOrderByAccountIdAsc(userId).stream()
                .filter(account -> !accountKeys.contains(accountKey(account)))
                .forEach(UserAccount::deactivate);

        Map<String, UserAccount> accountsByExternalId = new LinkedHashMap<>();
        for (MydataAccountData source : accountData) {
            UserAccount account = accountRepository
                    .findByUserUserIdAndOrgCodeAndAccountNum(
                            userId, source.getOrgCode(), source.getAccountNum())
                    .orElseGet(() -> UserAccount.builder()
                            .user(user)
                            .orgCode(source.getOrgCode())
                            .accountNum(source.getAccountNum())
                            .accountName(source.getAccountName())
                            .productName(source.getProductName())
                            .accountType(source.getAccountType())
                            .balance(source.getBalance())
                            .availableBalance(source.getAvailableBalance())
                            .interestRate(source.getInterestRate())
                            .build());
            account.sync(source.getAccountName(), source.getProductName(), source.getAccountType(),
                    source.getBalance(), source.getAvailableBalance(), source.getInterestRate(),
                    syncedAt);
            accountsByExternalId.put(source.getExternalAccountId(), accountRepository.save(account));
        }

        Set<String> cardKeys = cardData.stream().map(this::cardKey).collect(Collectors.toSet());
        cardRepository.findAllByUserUserId(userId).stream()
                .filter(card -> !cardKeys.contains(cardKey(card)))
                .forEach(UserCard::deactivate);

        Map<String, UserCard> cardsByExternalId = new LinkedHashMap<>();
        for (MydataCardData source : cardData) {
            UserCard card = cardRepository
                    .findByUserUserIdAndOrgCodeAndCardIdentifier(
                            userId, source.getOrgCode(), source.getExternalCardId())
                    .orElseGet(() -> UserCard.builder()
                            .user(user)
                            .orgCode(source.getOrgCode())
                            .cardIdentifier(source.getExternalCardId())
                            .cardName(source.getCardName())
                            .cardType(source.getCardType())
                            .creditLimit(source.getCreditLimit())
                            .build());
            card.sync(source.getCardName(), source.getCardType(), source.getCreditLimit(),
                    syncedAt);
            cardsByExternalId.put(source.getExternalCardId(), cardRepository.save(card));
        }
        return new MydataSourceSnapshot(accountsByExternalId, cardsByExternalId);
    }

    /**
     * 신규 거래만 저장하고, 전체 INSERT가 끝난 다음 취소·환불 행을 원거래와 연결한다.
     * 원거래가 응답 목록의 뒤쪽에 있어도 연결할 수 있도록 두 단계로 나눈다.
     */
    @Transactional
    public MydataWriteResult saveTransactions(
            List<MydataTransactionBatch.AccountBatch> accountBatches,
            List<MydataTransactionBatch.CardBatch> cardBatches,
            LocalDateTime syncedAt) {
        MydataWriteResult result = new MydataWriteResult();
        Map<String, UserAccount> accountsByNumber = accountBatches.stream()
                .map(MydataTransactionBatch.AccountBatch::getAccount)
                .collect(Collectors.toMap(UserAccount::getAccountNum, Function.identity(), (a, b) -> a));

        for (MydataTransactionBatch.AccountBatch batch : accountBatches) {
            saveAccountBatch(batch, accountsByNumber, syncedAt, result);
        }
        for (MydataTransactionBatch.CardBatch batch : cardBatches) {
            saveCardBatch(batch, syncedAt, result);
        }
        return result;
    }

    private void saveAccountBatch(MydataTransactionBatch.AccountBatch batch,
                                  Map<String, UserAccount> accountsByNumber,
                                  LocalDateTime syncedAt, MydataWriteResult result) {
        List<String> sourceIds = new ArrayList<>();
        batch.getTransactions().forEach(source -> {
            sourceIds.add(source.getTransactionId());
            // 원거래가 이번 조회 기간 밖에 있어도 이미 DB에 저장돼 있다면 연결할 수 있어야 한다.
            if (source.getOriginalTransactionId() != null) {
                sourceIds.add(source.getOriginalTransactionId());
            }
        });
        Map<String, FinancialTransaction> known = existingAccountTransactions(
                batch.getAccount().getAccountId(), sourceIds);
        List<PendingOriginalLink> pendingLinks = new ArrayList<>();

        for (MydataAccountTransactionData source : batch.getTransactions()) {
            FinancialTransaction transaction = known.get(source.getTransactionId());
            if (transaction != null) {
                result.addSkipped();
                continue;
            }
            UserAccount counterparty = accountsByNumber.get(source.getCounterpartyAccountNum());
            TransactionType type = accountTransactionType(source.getTransactionType(), counterparty);
            transaction = transactionRepository.save(FinancialTransaction.builder()
                    .sourceType(TransactionSourceType.ACCOUNT)
                    .account(batch.getAccount())
                    .sourceTransactionId(source.getTransactionId())
                    .counterpartyAccount(type == TransactionType.SELF_TRANSFER ? counterparty : null)
                    .transactionAt(source.getTransactionAt())
                    .syncedAt(syncedAt)
                    .transactionType(type)
                    .transactionStatus(source.getStatus())
                    .amount(source.getAmount())
                    .merchantName(source.getMerchantName())
                    .merchantRegno(source.getMerchantRegno())
                    .transMemo(source.getMemo())
                    .build());
            known.put(source.getTransactionId(), transaction);
            pendingLinks.add(new PendingOriginalLink(transaction, source.getOriginalTransactionId()));
            result.addInserted();
        }
        linkOriginals(known, pendingLinks, result);
    }

    private void saveCardBatch(MydataTransactionBatch.CardBatch batch, LocalDateTime syncedAt,
                               MydataWriteResult result) {
        List<String> sourceIds = new ArrayList<>();
        batch.getTransactions().forEach(source -> {
            sourceIds.add(source.getTransactionId());
            if (source.getOriginalTransactionId() != null) {
                sourceIds.add(source.getOriginalTransactionId());
            }
        });
        Map<String, FinancialTransaction> known = existingCardTransactions(
                batch.getCard().getCardId(), sourceIds);
        List<PendingOriginalLink> pendingLinks = new ArrayList<>();

        for (MydataCardTransactionData source : batch.getTransactions()) {
            FinancialTransaction transaction = known.get(source.getTransactionId());
            if (transaction != null) {
                result.addSkipped();
                continue;
            }
            transaction = transactionRepository.save(FinancialTransaction.builder()
                    .sourceType(TransactionSourceType.CARD)
                    .card(batch.getCard())
                    .sourceTransactionId(source.getTransactionId())
                    .transactionAt(source.getTransactionAt())
                    .syncedAt(syncedAt)
                    .transactionType(TransactionType.EXPENSE)
                    .transactionStatus(source.getStatus())
                    .amount(source.getAmount())
                    .merchantName(source.getMerchantName())
                    .merchantRegno(source.getMerchantRegno())
                    .build());
            known.put(source.getTransactionId(), transaction);
            pendingLinks.add(new PendingOriginalLink(transaction, source.getOriginalTransactionId()));
            result.addInserted();
        }
        linkOriginals(known, pendingLinks, result);
    }

    private Map<String, FinancialTransaction> existingAccountTransactions(Long accountId,
                                                                           List<String> ids) {
        if (ids.isEmpty()) return new HashMap<>();
        return transactionRepository.findAllByAccountAccountIdAndSourceTransactionIdIn(accountId, ids)
                .stream().collect(Collectors.toMap(
                        FinancialTransaction::getSourceTransactionId, Function.identity()));
    }

    private Map<String, FinancialTransaction> existingCardTransactions(Long cardId,
                                                                        List<String> ids) {
        if (ids.isEmpty()) return new HashMap<>();
        return transactionRepository.findAllByCardCardIdAndSourceTransactionIdIn(cardId, ids)
                .stream().collect(Collectors.toMap(
                        FinancialTransaction::getSourceTransactionId, Function.identity()));
    }

    private void linkOriginals(Map<String, FinancialTransaction> known,
                               List<PendingOriginalLink> pendingLinks,
                               MydataWriteResult result) {
        for (PendingOriginalLink pending : pendingLinks) {
            if (pending.originalSourceId == null) continue;
            FinancialTransaction original = known.get(pending.originalSourceId);
            if (original != null) {
                pending.transaction.markStatus(pending.transaction.getTransactionStatus(), original);
                result.addLinkedCancellation();
            }
        }
    }

    private TransactionType accountTransactionType(String sourceType, UserAccount counterparty) {
        if ("TRANSFER".equals(sourceType)) {
            // 상대 계좌번호가 이번 사용자의 계좌 목록에 있을 때만 내부 이체로 확정한다.
            return counterparty != null ? TransactionType.SELF_TRANSFER : TransactionType.OTHER;
        }
        return TransactionType.valueOf(sourceType);
    }

    private String accountKey(MydataAccountData source) {
        return source.getOrgCode() + "\u0000" + source.getAccountNum();
    }

    private String accountKey(UserAccount account) {
        return account.getOrgCode() + "\u0000" + account.getAccountNum();
    }

    private String cardKey(MydataCardData source) {
        return source.getOrgCode() + "\u0000" + source.getExternalCardId();
    }

    private String cardKey(UserCard card) {
        return card.getOrgCode() + "\u0000" + card.getCardIdentifier();
    }

    /** 새 거래와 원본 외부 ID를 잠시 묶어 두는 저장 단계 전용 값이다. */
    private static class PendingOriginalLink {
        private final FinancialTransaction transaction;
        private final String originalSourceId;

        private PendingOriginalLink(FinancialTransaction transaction, String originalSourceId) {
            this.transaction = transaction;
            this.originalSourceId = originalSourceId;
        }
    }
}
