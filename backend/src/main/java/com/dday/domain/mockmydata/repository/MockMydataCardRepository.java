package com.dday.domain.mockmydata.repository;

import com.dday.domain.mockmydata.entity.MockMydataCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockMydataCardRepository extends JpaRepository<MockMydataCard, Long> {

    List<MockMydataCard> findAllByMockUserServiceUserIdAndActiveTrueOrderByMockCardId(
            Long serviceUserId);

    Optional<MockMydataCard> findByExternalCardIdAndActiveTrue(String externalCardId);
}
