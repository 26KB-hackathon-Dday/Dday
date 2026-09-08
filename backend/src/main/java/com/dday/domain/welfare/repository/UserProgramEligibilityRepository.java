package com.dday.domain.welfare.repository;

import com.dday.domain.welfare.entity.UserProgramEligibility;
import com.dday.domain.welfare.entity.UserProgramId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserProgramEligibilityRepository
        extends JpaRepository<UserProgramEligibility, UserProgramId> {

    /** SUBSIDY-004 "놓치고 있는 제도" — 자격은 되는데 아직 안 받는 것 후보. idx_user_eligible. */
    List<UserProgramEligibility> findByIdUserIdAndEligibleTrue(Long userId);

    List<UserProgramEligibility> findByIdUserId(Long userId);
}
