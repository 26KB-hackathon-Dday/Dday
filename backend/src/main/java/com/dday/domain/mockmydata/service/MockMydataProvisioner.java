package com.dday.domain.mockmydata.service;

import com.dday.domain.mockmydata.entity.*;
import com.dday.domain.mockmydata.repository.*;
import com.dday.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 새로 가입한 회원에게 <b>데모용 금융 데이터를 붙여준다.</b>
 *
 * <p>시연에서 누가 가입하든 같은 계좌·카드·거래가 보여야 하는데, 목데이터는 시드에 있는
 * 한 사람({@value #TEMPLATE_SERVICE_USER_ID}) 것뿐이다. 그래서 그 사람의 데이터를
 * <b>원본으로 삼아 복제</b>한다.
 *
 * <p><b>코드에 금액·가맹점을 다시 적지 않고 DB의 원본을 읽어 복제하는 이유</b>는,
 * 하드코딩하면 {@code data.sql}의 시드와 두 벌이 되어 한쪽만 고쳤을 때 조용히 어긋나기
 * 때문이다. 데모 데이터를 바꾸고 싶으면 {@code data.sql} 한 곳만 고치면 된다.
 *
 * <p><b>⚠️ 해커톤 시연 전용이다.</b> 실제 마이데이터를 붙이면 이 클래스와 호출부를 지운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockMydataProvisioner {

    /**
     * 복제 원본이 되는 데모 계정의 이메일. {@code data.sql}의 시드와 같아야 한다.
     *
     * <p><b>user_id가 아니라 이메일로 찾는다.</b> 시드는 9001을 넣으려 하지만 그 이메일이
     * 이미 다른 id로 가입돼 있으면 유니크 키에 걸려 기존 행이 갱신될 뿐이라 id를 단정할 수 없다.
     */
    public static final String TEMPLATE_EMAIL = "user1@test.com";

    private final UserRepository serviceUserRepository;
    private final MockMydataUserRepository userRepository;
    private final MockMydataAccountRepository accountRepository;
    private final MockMydataCardRepository cardRepository;
    private final MockMydataAccountTransactionRepository accountTransactionRepository;
    private final MockMydataCardTransactionRepository cardTransactionRepository;

    /**
     * 이 회원의 목데이터를 만든다. <b>이미 있으면 아무것도 하지 않는다</b> —
     * 마이데이터 연결을 두 번 눌러도 계좌가 두 배가 되면 안 된다.
     *
     * <p>원본이 없으면(시드를 안 넣은 DB) 조용히 건너뛴다. 데모 데이터가 없다고 해서
     * 회원가입이나 연결 자체가 실패하면 안 되기 때문이다.
     *
     * @return 실제로 만들었으면 true, 건너뛰었으면 false
     */
    @Transactional
    public boolean provision(Long serviceUserId, String name) {
        if (serviceUserId == null) {
            return false;
        }
        if (userRepository.findByServiceUserId(serviceUserId).isPresent()) {
            return false;   // 이미 붙어 있다
        }

        Long templateUserId = serviceUserRepository.findByEmail(TEMPLATE_EMAIL)
                .map(u -> u.getUserId())
                .orElse(null);
        if (templateUserId == null || templateUserId.equals(serviceUserId)) {
            // 원본이 없거나(시드를 안 넣은 DB) 원본 본인이면 복제할 게 없다.
            return false;
        }

        MockMydataUser template = userRepository.findByServiceUserId(templateUserId).orElse(null);
        if (template == null) {
            log.warn("목데이터 원본({})이 없어 복제를 건너뛴다. data.sql 시드를 확인할 것.",
                    TEMPLATE_EMAIL);
            return false;
        }

        MockMydataUser mockUser = userRepository.save(MockMydataUser.builder()
                .serviceUserId(serviceUserId)
                .name(name)
                .build());

        copyAccounts(template, mockUser, serviceUserId);
        copyCards(template, mockUser, serviceUserId);

        log.info("데모 목데이터를 복제했다: serviceUserId={}", serviceUserId);
        return true;
    }

    private void copyAccounts(MockMydataUser template, MockMydataUser target, Long serviceUserId) {
        List<MockMydataAccount> sources = accountRepository
                .findAllByMockUserServiceUserIdAndActiveTrueOrderByMockAccountId(
                        template.getServiceUserId());

        for (MockMydataAccount source : sources) {
            MockMydataAccount copy = accountRepository.save(MockMydataAccount.builder()
                    .mockUser(target)
                    .externalAccountId(scoped(serviceUserId, source.getExternalAccountId()))
                    .orgCode(source.getOrgCode())
                    .accountNum(source.getAccountNum())
                    .accountName(source.getAccountName())
                    .productName(source.getProductName())
                    .accountType(source.getAccountType())
                    .balance(source.getBalance())
                    .availableBalance(source.getAvailableBalance())
                    .build());

            copyAccountTransactions(source, copy, serviceUserId);
        }
    }

    private void copyAccountTransactions(MockMydataAccount source, MockMydataAccount target,
                                         Long serviceUserId) {
        for (MockMydataAccountTransaction tx : accountTransactionRepository
                .findAllByMockAccountMockAccountIdOrderByMockTransactionId(source.getMockAccountId())) {

            MockMydataAccountTransaction copy = MockMydataAccountTransaction.builder()
                    .mockAccount(target)
                    .externalTransactionId(scoped(serviceUserId, tx.getExternalTransactionId()))
                    .transactionAt(tx.getTransactionAt())
                    .transactionType(tx.getTransactionType())
                    .amount(tx.getAmount())
                    .counterpartyName(tx.getCounterpartyName())
                    .counterpartyAccountNum(tx.getCounterpartyAccountNum())
                    .merchantName(tx.getMerchantName())
                    .merchantRegno(tx.getMerchantRegno())
                    .transMemo(tx.getTransMemo())
                    .originalTransactionId(scopedNullable(serviceUserId, tx.getOriginalTransactionId()))
                    .build();

            // 빌더는 상태를 NORMAL로 고정한다. 취소·환불 거래를 그대로 옮기려면 따로 찍어야 한다.
            applyStatus(copy, tx.getTransactionStatus(),
                    scopedNullable(serviceUserId, tx.getOriginalTransactionId()));

            accountTransactionRepository.save(copy);
        }
    }

    private void copyCards(MockMydataUser template, MockMydataUser target, Long serviceUserId) {
        List<MockMydataCard> sources = cardRepository
                .findAllByMockUserServiceUserIdAndActiveTrueOrderByMockCardId(
                        template.getServiceUserId());

        for (MockMydataCard source : sources) {
            MockMydataCard copy = cardRepository.save(MockMydataCard.builder()
                    .mockUser(target)
                    .externalCardId(scoped(serviceUserId, source.getExternalCardId()))
                    .orgCode(source.getOrgCode())
                    .cardName(source.getCardName())
                    .cardType(source.getCardType())
                    .creditLimit(source.getCreditLimit())
                    .build());

            copyCardTransactions(source, copy, serviceUserId);
        }
    }

    private void copyCardTransactions(MockMydataCard source, MockMydataCard target,
                                      Long serviceUserId) {
        for (MockMydataCardTransaction tx : cardTransactionRepository
                .findAllByMockCardMockCardIdOrderByMockCardTransactionId(source.getMockCardId())) {

            MockMydataCardTransaction copy = MockMydataCardTransaction.builder()
                    .mockCard(target)
                    .externalTransactionId(scoped(serviceUserId, tx.getExternalTransactionId()))
                    .transactionAt(tx.getTransactionAt())
                    .amount(tx.getAmount())
                    .merchantName(tx.getMerchantName())
                    .merchantRegno(tx.getMerchantRegno())
                    .originalTransactionId(scopedNullable(serviceUserId, tx.getOriginalTransactionId()))
                    .build();

            applyStatus(copy, tx.getTransactionStatus(),
                    scopedNullable(serviceUserId, tx.getOriginalTransactionId()));

            cardTransactionRepository.save(copy);
        }
    }

    private void applyStatus(MockMydataAccountTransaction tx, MockTransactionStatus status,
                             String originalTransactionId) {
        if (status != MockTransactionStatus.NORMAL) {
            tx.markStatus(status, originalTransactionId);
        }
    }

    private void applyStatus(MockMydataCardTransaction tx, MockTransactionStatus status,
                            String originalTransactionId) {
        if (status != MockTransactionStatus.NORMAL) {
            tx.markStatus(status, originalTransactionId);
        }
    }

    /**
     * 외부 식별자에 회원 구분을 붙인다.
     *
     * <p>원본 값을 그대로 복제하면 안 된다 — 목 API가 {@code externalAccountId} 하나로
     * 계좌를 찾는데({@code findByExternalAccountIdAndActiveTrue}), 여러 회원이 같은 값을
     * 가지면 남의 계좌가 잡힌다.
     */
    private String scoped(Long serviceUserId, String externalId) {
        return "U" + serviceUserId + "-" + externalId;
    }

    /** 취소 거래가 가리키는 원거래 id도 같이 바꿔야 복제본 안에서 짝이 맞는다. */
    private String scopedNullable(Long serviceUserId, String externalId) {
        return externalId == null ? null : scoped(serviceUserId, externalId);
    }
}
