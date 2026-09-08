package com.dday.domain.mydata.repository;

import com.dday.domain.mydata.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 동기화 과정에서 사용자 계좌를 외부 복합 식별자로 찾고 저장한다. */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUserUserIdAndOrgCodeAndAccountNum(
            Long userId, String orgCode, String accountNum);

    List<UserAccount> findAllByUserUserId(Long userId);
}
