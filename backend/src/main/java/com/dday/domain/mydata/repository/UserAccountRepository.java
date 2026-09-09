package com.dday.domain.mydata.repository;

import com.dday.domain.mydata.entity.UserAccount;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 마이데이터 사용자 계좌 Repository.
 */
public interface UserAccountRepository
        extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount>
    findByUserUserIdAndOrgCodeAndAccountNum(
            Long userId,
            String orgCode,
            String accountNum
    );

    /**
<<<<<<< HEAD
     * 표시 순서가 화면에서 바뀌지 않도록
     * 등록 순서(accountId) 기준으로 조회한다.
=======
     * 연동 직후 응답(연결된 기관·계좌 수)에 쓴다. <b>비활성 계좌를 뺀다</b> — 안 그러면
     * 기관을 정리해도(예: 신한 계좌 삭제) 예전에 연동했던 회원은 죽은 계좌까지 세어
     * "연결된 기관 수"가 실제보다 많게 나온다.
     */
    List<UserAccount> findAllByUserUserIdAndActiveTrueOrderByAccountIdAsc(Long userId);

    /**
     * <b>id만으로 찾지 않고 소유자를 함께 건다.</b> 마이페이지에서 계좌 선택여부를 바꿀 때
     * 이 메서드를 쓰면 남의 계좌를 건드리는 요청이 조회 단계에서 빈 값이 되어 애초에 진행되지 않는다.
>>>>>>> main
     */
    List<UserAccount>
    findAllByUserUserIdOrderByAccountIdAsc(
            Long userId
    );

    /**
     * 계좌 id + 사용자 id로 함께 조회.
     */
    Optional<UserAccount>
    findByAccountIdAndUserUserId(
            Long accountId,
            Long userId
    );

    /**
     * 특정 기관에 연결된 사용자의 모든 계좌.
     */
    List<UserAccount>
    findAllByUserUserIdAndOrgCode(
            Long userId,
            String orgCode
    );

    /**
     * 사용자의 활성 계좌 존재 여부.
     */
    boolean existsByUserUserIdAndActiveTrue(
            Long userId
    );

    /**
     * 예상 자산 계산용.
     *
     * 사용자가 예산 계산 대상으로 선택했고,
     * 현재 활성 상태인 계좌의 잔액을 모두 더한다.
     *
     * 선택된 계좌가 없으면 0이 반환된다.
     */
    @Query("""
            select coalesce(sum(a.balance), 0)
            from UserAccount a
            where a.user.userId = :userId
              and a.selected = true
              and a.active = true
            """)
    Long sumSelectedActiveBalance(
            @Param("userId")
            Long userId
    );
}