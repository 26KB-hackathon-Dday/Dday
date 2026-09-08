package com.dday.domain.mockmydata.service;

import com.dday.domain.mockmydata.dto.MockMydataErrorCode;
import com.dday.domain.mockmydata.dto.response.*;
import com.dday.domain.mockmydata.entity.MockMydataAccount;
import com.dday.domain.mockmydata.entity.MockMydataCard;
import com.dday.domain.mockmydata.repository.*;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MockMydataService {

    private final MockMydataUserRepository userRepository;
    private final MockMydataAccountRepository accountRepository;
    private final MockMydataCardRepository cardRepository;
    private final MockMydataAccountTransactionRepository accountTransactionRepository;
    private final MockMydataCardTransactionRepository cardTransactionRepository;

    @Transactional(readOnly = true)
    public MockAccountListResponse findAccounts(Long serviceUserId) {
        requireUser(serviceUserId);
        return MockAccountListResponse.builder()
                .accounts(accountRepository
                        .findAllByMockUserServiceUserIdAndActiveTrueOrderByMockAccountId(serviceUserId)
                        .stream()
                        .map(MockAccountResponse::from)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public MockCardListResponse findCards(Long serviceUserId) {
        requireUser(serviceUserId);
        return MockCardListResponse.builder()
                .cards(cardRepository
                        .findAllByMockUserServiceUserIdAndActiveTrueOrderByMockCardId(serviceUserId)
                        .stream()
                        .map(MockCardResponse::from)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public MockAccountTransactionListResponse findAccountTransactions(
            String externalAccountId, LocalDateTime from, LocalDateTime to) {
        validatePeriod(from, to);
        MockMydataAccount account = accountRepository
                .findByExternalAccountIdAndActiveTrue(externalAccountId)
                .orElseThrow(() -> new BusinessException(
                        MockMydataErrorCode.MOCK_MYDATA_ACCOUNT_NOT_FOUND));
        return MockAccountTransactionListResponse.builder()
                .transactions(accountTransactionRepository
                        .findInPeriod(account.getMockAccountId(), from, to)
                        .stream()
                        .map(MockAccountTransactionResponse::from)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public MockCardTransactionListResponse findCardTransactions(
            String externalCardId, LocalDateTime from, LocalDateTime to) {
        validatePeriod(from, to);
        MockMydataCard card = cardRepository.findByExternalCardIdAndActiveTrue(externalCardId)
                .orElseThrow(() -> new BusinessException(
                        MockMydataErrorCode.MOCK_MYDATA_CARD_NOT_FOUND));
        return MockCardTransactionListResponse.builder()
                .transactions(cardTransactionRepository
                        .findInPeriod(card.getMockCardId(), from, to)
                        .stream()
                        .map(MockCardTransactionResponse::from)
                        .toList())
                .build();
    }

    private void requireUser(Long serviceUserId) {
        if (serviceUserId == null || userRepository.findByServiceUserId(serviceUserId).isEmpty()) {
            throw new BusinessException(MockMydataErrorCode.MOCK_MYDATA_USER_NOT_FOUND);
        }
    }

    private void validatePeriod(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new BusinessException(MockMydataErrorCode.INVALID_TRANSACTION_PERIOD);
        }
    }
}
