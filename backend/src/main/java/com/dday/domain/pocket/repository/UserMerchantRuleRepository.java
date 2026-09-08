package com.dday.domain.pocket.repository;

import com.dday.domain.pocket.entity.UserMerchantRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** 사용자별 가맹점 키에 연결된 포켓·카테고리 규칙을 관리한다. */
public interface UserMerchantRuleRepository extends JpaRepository<UserMerchantRule, Long> {

    /** 자동 분류에서 지연 로딩 추가 쿼리가 발생하지 않도록 결과 연관을 함께 가져온다. */
    @Query("""
            select rule
            from UserMerchantRule rule
            join fetch rule.category
            join fetch rule.pocket
            where rule.user.userId = :userId
              and rule.merchantKey = :merchantKey
            """)
    Optional<UserMerchantRule> findRule(@Param("userId") Long userId,
                                        @Param("merchantKey") String merchantKey);
}
