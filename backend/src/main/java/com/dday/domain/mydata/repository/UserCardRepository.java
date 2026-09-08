package com.dday.domain.mydata.repository;

import com.dday.domain.mydata.entity.UserCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 실제 카드번호 대신 외부 카드 식별값을 사용해 사용자 카드를 조회한다. */
public interface UserCardRepository extends JpaRepository<UserCard, Long> {

    Optional<UserCard> findByUserUserIdAndOrgCodeAndCardIdentifier(
            Long userId, String orgCode, String cardIdentifier);

    List<UserCard> findAllByUserUserId(Long userId);
}
