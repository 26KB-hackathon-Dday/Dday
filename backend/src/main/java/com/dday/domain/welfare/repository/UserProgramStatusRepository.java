package com.dday.domain.welfare.repository;

import com.dday.domain.welfare.entity.ReceivingStatus;
import com.dday.domain.welfare.entity.UserProgramId;
import com.dday.domain.welfare.entity.UserProgramStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserProgramStatusRepository
        extends JpaRepository<UserProgramStatus, UserProgramId> {

    List<UserProgramStatus> findByIdUserId(Long userId);

    /** 홈 집계 — 자격 있는 제도들의 수급 상태만 골라 온다. */
    List<UserProgramStatus> findByIdUserIdAndIdProgramIdIn(Long userId, Collection<String> programIds);

    /** SUBSIDY-004 필터 — 이미 받고 있는 건 "놓치고 있는 제도"에서 뺀다. idx_user_receiving. */
    List<UserProgramStatus> findByIdUserIdAndReceivingStatus(Long userId, ReceivingStatus receivingStatus);

    /** SUBSIDY-008 관심 제도 목록. idx_user_favorite. */
    List<UserProgramStatus> findByIdUserIdAndFavoriteTrue(Long userId);
}
