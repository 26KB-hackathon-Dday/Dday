package com.dday.domain.mockmydata.repository;

import com.dday.domain.mockmydata.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MockMydataRepositoryTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 8, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 9, 1, 0, 0);

    @Autowired
    private MockMydataUserRepository userRepository;
    @Autowired
    private MockMydataAccountRepository accountRepository;
    @Autowired
    private MockMydataAccountTransactionRepository transactionRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void 사용자_계좌_조회는_활성_계좌만_반환한다() {
        MockMydataUser user = persist(MockMydataUser.builder()
                .serviceUserId(System.nanoTime())
                .name("Mock 사용자")
                .build());
        MockMydataAccount active = persist(account(user, "ACTIVE-" + System.nanoTime()));
        MockMydataAccount inactive = persist(account(user, "INACTIVE-" + System.nanoTime()));
        inactive.deactivate();
        entityManager.flush();
        entityManager.clear();

        var result = accountRepository
                .findAllByMockUserServiceUserIdAndActiveTrueOrderByMockAccountId(user.getServiceUserId());

        assertThat(result).extracting(MockMydataAccount::getExternalAccountId)
                .containsExactly(active.getExternalAccountId());
        assertThat(userRepository.findByServiceUserId(user.getServiceUserId())).isPresent();
    }

    @Test
    void 계좌_거래는_반개구간으로_조회하고_최신순을_유지한다() {
        MockMydataUser user = persist(MockMydataUser.builder()
                .serviceUserId(System.nanoTime())
                .name("Mock 사용자")
                .build());
        MockMydataAccount account = persist(account(user, "PERIOD-" + System.nanoTime()));
        persist(transaction(account, "BEFORE", FROM.minusSeconds(1)));
        persist(transaction(account, "FROM", FROM));
        persist(transaction(account, "MIDDLE-1", FROM.plusDays(1)));
        persist(transaction(account, "MIDDLE-2", FROM.plusDays(1)));
        persist(transaction(account, "TO", TO));
        entityManager.flush();
        entityManager.clear();

        var result = transactionRepository.findInPeriod(account.getMockAccountId(), FROM, TO);

        assertThat(result).extracting(MockMydataAccountTransaction::getExternalTransactionId)
                .containsExactly("MIDDLE-2", "MIDDLE-1", "FROM");
    }

    private MockMydataAccount account(MockMydataUser user, String externalAccountId) {
        return MockMydataAccount.builder()
                .mockUser(user)
                .externalAccountId(externalAccountId)
                .orgCode("0004")
                .accountNum(externalAccountId)
                .accountType(MockAccountType.DEPOSIT)
                .build();
    }

    private MockMydataAccountTransaction transaction(
            MockMydataAccount account, String externalTransactionId, LocalDateTime transactionAt) {
        return MockMydataAccountTransaction.builder()
                .mockAccount(account)
                .externalTransactionId(externalTransactionId)
                .transactionAt(transactionAt)
                .transactionType(MockTransactionType.EXPENSE)
                .amount(1_000L)
                .build();
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
