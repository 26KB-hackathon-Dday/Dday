package com.dday.domain.mockmydata.repository;

import com.dday.domain.mockmydata.entity.MockMydataAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockMydataAccountRepository extends JpaRepository<MockMydataAccount, Long> {

    List<MockMydataAccount> findAllByMockUserServiceUserIdAndActiveTrueOrderByMockAccountId(
            Long serviceUserId);

    Optional<MockMydataAccount> findByExternalAccountIdAndActiveTrue(String externalAccountId);
}
