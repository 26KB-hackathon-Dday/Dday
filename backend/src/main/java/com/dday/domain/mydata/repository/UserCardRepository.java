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

    /** 표시 순서가 화면에서 바뀌면 안 되니 등록 순서(id)로 고정한다. */
    List<UserCard> findAllByUserUserIdOrderByCardIdAsc(Long userId);

    /** 기관 연결 해제 대상을 찾는다. 같은 기관 카드가 여러 개일 수 있어 리스트다. */
    List<UserCard> findAllByUserUserIdAndOrgCode(Long userId, String orgCode);

    /** 회원의 마이데이터 연결이 전부 끊겼는지 판단할 때 쓴다. */
    boolean existsByUserUserIdAndActiveTrue(Long userId);
}
