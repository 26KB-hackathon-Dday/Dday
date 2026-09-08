package com.dday.domain.mockmydata.repository;

import com.dday.domain.mockmydata.entity.MockMydataUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MockMydataUserRepository extends JpaRepository<MockMydataUser, Long> {

    Optional<MockMydataUser> findByServiceUserId(Long serviceUserId);
}
