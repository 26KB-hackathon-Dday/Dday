package com.dday.domain.welfare.repository;

import com.dday.domain.welfare.entity.UserProgramEligibility;
import com.dday.domain.welfare.entity.UserProgramId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserProgramEligibilityRepository
        extends JpaRepository<UserProgramEligibility, UserProgramId> {

    /** SUBSIDY-004 "놓치고 있는 제도" — 자격은 되는데 아직 안 받는 것 후보. idx_user_eligible. */
    List<UserProgramEligibility> findByIdUserIdAndEligibleTrue(Long userId);

    List<UserProgramEligibility> findByIdUserId(Long userId);

    /**
     * 홈 상단 "마지막 업데이트" 시각. 자격 유무와 무관하게 이 유저의 마지막 판별 시각을 본다
     * (적격 행만 훑으면, 적격이 하나도 없는 유저는 판별을 돌렸는데도 {@code null}이 된다).
     * 판별 이력이 없으면 {@code null}.
     */
    @Query("select max(e.evaluatedAt) from UserProgramEligibility e where e.id.userId = :userId")
    LocalDateTime findLastEvaluatedAt(@Param("userId") Long userId);
}
