package com.dday.domain.mydata.client;

import com.dday.domain.mockmydata.dto.response.*;
import com.dday.domain.mockmydata.service.MockMydataService;
import com.dday.domain.mydata.client.dto.*;
import com.dday.domain.mydata.dto.MydataErrorCode;
import com.dday.domain.mydata.entity.*;
import com.dday.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 로컬 Mock MyData 공급자를 {@link MydataClient} 계약으로 변환한다.
 *
 * <p>같은 프로세스 안에서 자기 자신을 HTTP로 다시 호출하지 않는다. Mock 서비스 호출도 외부
 * 공급자 어댑터 안에 가두므로, 실제 HTTP 구현으로 교체해도 동기화 서비스의 계약은 바뀌지 않는다.
 */
@Component
@RequiredArgsConstructor
public class MockMydataClient implements MydataClient {

    private final MockMydataService mockMydataService;

    @Override
    public List<MydataAccountData> getAccounts(Long userId) {
        try {
            return mockMydataService.findAccounts(userId).getAccounts().stream()
                    .map(this::account)
                    .toList();
        } catch (BusinessException exception) {
            throw new BusinessException(MydataErrorCode.MYDATA_CLIENT_FAILURE);
        }
    }

    @Override
    public List<MydataCardData> getCards(Long userId) {
        try {
            return mockMydataService.findCards(userId).getCards().stream()
                    .map(this::card)
                    .toList();
        } catch (BusinessException exception) {
            throw new BusinessException(MydataErrorCode.MYDATA_CLIENT_FAILURE);
        }
    }

    @Override
    public List<MydataAccountTransactionData> getAccountTransactions(
            String externalAccountId, LocalDateTime from, LocalDateTime to) {
        try {
            return mockMydataService.findAccountTransactions(externalAccountId, from, to)
                    .getTransactions().stream().map(this::accountTransaction).toList();
        } catch (BusinessException exception) {
            throw new BusinessException(MydataErrorCode.MYDATA_CLIENT_FAILURE);
        }
    }

    @Override
    public List<MydataCardTransactionData> getCardTransactions(
            String externalCardId, LocalDateTime from, LocalDateTime to) {
        try {
            return mockMydataService.findCardTransactions(externalCardId, from, to)
                    .getTransactions().stream().map(this::cardTransaction).toList();
        } catch (BusinessException exception) {
            throw new BusinessException(MydataErrorCode.MYDATA_CLIENT_FAILURE);
        }
    }

    private MydataAccountData account(MockAccountResponse source) {
        return MydataAccountData.builder()
                .externalAccountId(source.getAccountId())
                .orgCode(source.getOrgCode())
                .accountNum(source.getAccountNum())
                .accountName(source.getAccountName())
                .productName(source.getProductName())
                .accountType(AccountType.valueOf(source.getAccountType().name()))
                .balance(source.getBalance())
                .availableBalance(source.getAvailableBalance())
                .interestRate(source.getInterestRate())
                .build();
    }

    private MydataCardData card(MockCardResponse source) {
        return MydataCardData.builder()
                .externalCardId(source.getCardId())
                .orgCode(source.getOrgCode())
                .cardName(source.getCardName())
                .cardType(CardType.valueOf(source.getCardType().name()))
                .creditLimit(source.getCreditLimit())
                .build();
    }

    private MydataAccountTransactionData accountTransaction(MockAccountTransactionResponse source) {
        return MydataAccountTransactionData.builder()
                .transactionId(source.getTransactionId())
                .transactionAt(source.getTransactionAt())
                .transactionType(source.getTransactionType().name())
                .amount(source.getAmount())
                .counterpartyAccountNum(source.getCounterpartyAccountNum())
                .merchantName(source.getMerchantName())
                .merchantRegno(source.getMerchantRegno())
                .memo(source.getMemo())
                .status(TransactionStatus.valueOf(source.getStatus().name()))
                .originalTransactionId(source.getOriginalTransactionId())
                .build();
    }

    private MydataCardTransactionData cardTransaction(MockCardTransactionResponse source) {
        return MydataCardTransactionData.builder()
                .transactionId(source.getTransactionId())
                .transactionAt(source.getTransactionAt())
                .amount(source.getAmount())
                .merchantName(source.getMerchantName())
                .merchantRegno(source.getMerchantRegno())
                .status(TransactionStatus.valueOf(source.getStatus().name()))
                .originalTransactionId(source.getOriginalTransactionId())
                .build();
    }
}
