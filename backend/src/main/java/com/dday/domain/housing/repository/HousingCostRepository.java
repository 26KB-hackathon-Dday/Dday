package com.dday.domain.housing.repository;

import com.dday.domain.housing.entity.HousingCost;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 주거비 조회. PK가 {@code user_id}라 {@code findById(userId)}가 곧 회원별 조회다.
 *
 * <p>온보딩 전이거나 주거비를 입력하지 않은 회원은 행이 없다. 호출부는 빈 값을
 * "아직 안 채움"으로 다루면 되고, 없다고 예외를 던질 일이 아니다.
 */
public interface HousingCostRepository extends JpaRepository<HousingCost, Long> {
}
