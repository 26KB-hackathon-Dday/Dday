package com.dday.domain.pocket.repository;

import com.dday.domain.pocket.entity.Category;
import com.dday.domain.pocket.entity.PocketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** 비활성 카테고리를 거래 조회 필터로 사용하는 것을 막기 위한 존재 확인이다. */
    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

    /** 수동 분류에서 삭제된 기준정보가 다시 선택되지 않도록 활성 상태까지 확인한다. */
    Optional<Category> findByCategoryIdAndActiveTrue(Long categoryId);

    /**
     * 활성 카테고리를 ID 순으로 조회하고 부모를 함께 로딩한다.
     *
     * <p>{@code pocketType}이 {@code null}이면 전체 활성 카테고리를 반환한다. 부모를 fetch join해
     * 서비스가 트리를 조립할 때 카테고리마다 추가 쿼리가 발생하지 않게 한다.
     */
    @Query("""
            select category
            from Category category
            left join fetch category.parentCategory
            where category.active = true
              and (:pocketType is null or category.defaultPocketType = :pocketType)
            order by category.categoryId
            """)
    List<Category> findAllActive(@Param("pocketType") PocketType pocketType);
}
