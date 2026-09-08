package com.dday.domain.pocket.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소비 카테고리. 거래 하나가 어떤 성격의 지출인지를 가른다.
 *
 * <p>기준정보라 사용자별이 아니라 전역으로 하나만 있다. {@code parentCategory}로 2단 구성이
 * 가능하다(예: 식비 &gt; 배달).
 *
 * <p>{@link #defaultPocketType}은 <b>필수(ESSENTIAL)와 자유(FREE) 둘만</b> 쓴다. 비상금·미래자산은
 * 소비 카테고리로 자동 분류되는 대상이 아니라 사용자가 의도해서 넣는 돈이기 때문이다.
 * (DDL의 {@code ENUM('ESSENTIAL','FREE')}가 이 제약을 들고 있다)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "category",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_category_code",
                columnNames = "category_code"
        ),
        indexes = @Index(name = "idx_category_parent", columnList = "parent_category_id")
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    /** 상위 카테고리. 최상위면 {@code null}이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    /** 코드가 정본이다. 이름은 바뀔 수 있어도 코드는 고정이라 규칙·통계가 이걸 잡는다. */
    @Column(name = "category_code", nullable = false, length = 50)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    /** 이 카테고리로 분류된 거래가 기본으로 떨어질 포켓. ESSENTIAL 또는 FREE만 들어간다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "default_pocket_type", nullable = false, length = 20)
    private PocketType defaultPocketType;

    /**
     * 쓰지 않게 된 카테고리는 지우지 않고 내린다. 지우면 그 카테고리로 분류해둔
     * 과거 거래가 전부 미분류로 돌아간다.
     */
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Builder
    private Category(Category parentCategory, String categoryCode, String categoryName,
                     PocketType defaultPocketType) {
        this.parentCategory = parentCategory;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.defaultPocketType = defaultPocketType;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
