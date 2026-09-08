package com.dday.domain.credit.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대출·카드 화면에 붙는 금융역량 개선 팁.
 *
 * <p>사용자별이 아니라 전역 기준정보다. 문구를 코드에 박지 않고 테이블에 둔 이유는
 * 배포 없이 고칠 수 있어야 하기 때문이다.
 *
 * <p>{@link #displayOrder}로 정렬한다. PK 순서에 기대면 팁 하나를 새로 넣을 때
 * 순서를 맞추려고 기존 행을 지웠다 다시 넣어야 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "capability_tip",
        indexes = @Index(
                name = "idx_capability_tip_type_order",
                columnList = "capability_type, display_order"
        )
)
public class CapabilityTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "capability_tip_id")
    private Long capabilityTipId;

    @Enumerated(EnumType.STRING)
    @Column(name = "capability_type", nullable = false, length = 20)
    private CapabilityType capabilityType;

    @Column(name = "display_order", nullable = false, columnDefinition = "TINYINT")
    private Integer displayOrder;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    /** 팁에 행동 버튼을 달 때만 채운다. 읽기만 하는 팁은 {@code null}이다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 30)
    private TipActionType actionType;

    @Builder
    private CapabilityTip(CapabilityType capabilityType, Integer displayOrder, String title,
                          String description, TipActionType actionType) {
        this.capabilityType = capabilityType;
        this.displayOrder = displayOrder;
        this.title = title;
        this.description = description;
        this.actionType = actionType;
    }
}
