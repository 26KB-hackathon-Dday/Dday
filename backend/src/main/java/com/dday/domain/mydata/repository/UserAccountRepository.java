package com.dday.domain.mydata.repository;

import com.dday.domain.mydata.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 동기화 과정에서 사용자 계좌를 외부 복합 식별자로 찾고 저장한다. */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUserUserIdAndOrgCodeAndAccountNum(
            Long userId, String orgCode, String accountNum);

    /** 표시 순서가 화면에서 바뀌면 안 되니 등록 순서(id)로 고정한다. */
    List<UserAccount> findAllByUserUserIdOrderByAccountIdAsc(Long userId);

    /**
     * 연동 직후 응답(연결된 기관·계좌 수)에 쓴다. <b>비활성 계좌를 뺀다</b> — 안 그러면
     * 기관을 정리해도(예: 신한 계좌 삭제) 예전에 연동했던 회원은 죽은 계좌까지 세어
     * "연결된 기관 수"가 실제보다 많게 나온다.
     */
    List<UserAccount> findAllByUserUserIdAndActiveTrueOrderByAccountIdAsc(Long userId);

    /**
     * <b>id만으로 찾지 않고 소유자를 함께 건다.</b> 마이페이지에서 계좌 선택여부를 바꿀 때
     * 이 메서드를 쓰면 남의 계좌를 건드리는 요청이 조회 단계에서 빈 값이 되어 애초에 진행되지 않는다.
     */
    Optional<UserAccount> findByAccountIdAndUserUserId(Long accountId, Long userId);

    /** 기관 연결 해제 대상을 찾는다. 같은 기관 계좌가 여러 개일 수 있어 리스트다. */
    List<UserAccount> findAllByUserUserIdAndOrgCode(Long userId, String orgCode);

    /** 회원의 마이데이터 연결이 전부 끊겼는지 판단할 때 쓴다. */
    boolean existsByUserUserIdAndActiveTrue(Long userId);
}
