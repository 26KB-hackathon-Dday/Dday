package com.dday.domain.assetforecast.repository;

import com.dday.domain.mydata.entity.AccountType;
import com.dday.domain.mydata.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface AssetForecastAccountRepository
        extends JpaRepository<UserAccount, Long> {

    /**
     * 현재 미래자산으로 볼 계좌의 잔액 합계.
     *
     * - 사용자 본인의 계좌
     * - 금융기관 기준 활성 계좌
     * - SAVINGS / INVESTMENT 계좌
     *
     * DEPOSIT은 생활비용 입출금 계좌일 수 있으므로 제외한다.
     */
    @Query("""
            select coalesce(sum(account.balance), 0)
            from UserAccount account
            where account.user.userId = :userId
              and account.active = true
              and account.accountType in :accountTypes
            """)
    Long sumCurrentFutureAsset(
            @Param("userId") Long userId,
            @Param("accountTypes") Collection<AccountType> accountTypes
    );

    @Query("""
            select coalesce(sum(account.balance), 0)
            from UserAccount account
            where account.user.userId = :userId
              and account.active = true
              and account.accountType = :accountType
            """)
    Long sumActiveBalanceByType(@Param("userId") Long userId,
                                @Param("accountType") AccountType accountType);
}
