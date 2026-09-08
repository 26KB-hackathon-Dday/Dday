package com.dday.domain.housing.entity;

import com.dday.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 회원의 주거비. 온보딩에서 받아 예산 초안의 고정지출로 쓴다.
 *
 * <p><b>보증금·월세·관리비를 한 테이블에 모은다.</b> 셋은 이사하면 함께 바뀌는 한 덩어리라
 * 갱신 지점이 하나여야 한다. (보증금만 {@code users}에 두면 이사할 때 두 곳을 고쳐야 하고,
 * 한쪽만 고친 상태가 조용히 남는다)
 *
 * <p><b>월 예상 주거비는 저장하지 않는다.</b> {@code monthlyRent + maintenanceFee}로 항상
 * 계산할 수 있는 값이라 컬럼으로 두면 월세만 고쳤을 때 어긋난다.
 * 계산은 {@code HousingCostResponse}가 응답을 만들 때 한다.
 *
 * <p>회원당 한 행이라 {@code user_id}가 곧 PK다({@link MapsId}). 별도 시퀀스를 두면
 * "한 회원에 주거비 두 건"이 물리적으로 가능해져 유니크 제약을 따로 걸어야 한다.
 *
 * <p>세 금액 모두 nullable이다 — 가족과 살면 월세가 없고, 자립생활관은 보증금이 없는 식으로
 * 주거형태({@code users.housing_type})에 따라 해당 없는 항목이 생긴다.
 * 0과 "해당 없음"은 다르므로 0으로 채우지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "housing_cost")
public class HousingCost {

    @Id
    @Column(name = "user_id")
    private Long userId;

    /**
     * PK를 회원 것과 공유한다. {@code @MapsId}가 {@link #userId}를 이 연관에서 채우므로
     * 애플리케이션이 id를 직접 넣지 않는다.
     */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** 보증금(원). 월세·전세 보증금 모두 여기에 담는다. */
    @Column(name = "deposit")
    private Long deposit;

    /** 월세(원). 전세·자가면 {@code null}이다. */
    @Column(name = "monthly_rent")
    private Long monthlyRent;

    /** 관리비(원). */
    @Column(name = "maintenance_fee")
    private Long maintenanceFee;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private HousingCost(User user, Long deposit, Long monthlyRent, Long maintenanceFee) {
        this.user = user;
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
        this.maintenanceFee = maintenanceFee;
    }

    /**
     * 주거비를 통째로 다시 쓴다.
     *
     * <p>{@code null}을 건너뛰지 않는다 — 이사하면 "월세 없음"으로 바뀔 수 있는데
     * 건너뛰면 옛 월세가 그대로 남는다. 화면이 세 값을 항상 함께 보내는 전제다.
     */
    public void update(Long deposit, Long monthlyRent, Long maintenanceFee) {
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
        this.maintenanceFee = maintenanceFee;
    }

    /**
     * 월 예상 주거비 = 월세 + 관리비. 저장하지 않고 매번 계산한다.
     *
     * <p>둘 다 {@code null}이면(가족과 거주 등) {@code null}을 돌려준다 —
     * 0원과 "주거비가 없음"은 화면에서 다르게 보여야 한다.
     */
    public Long estimatedMonthly() {
        if (monthlyRent == null && maintenanceFee == null) {
            return null;
        }
        return nullToZero(monthlyRent) + nullToZero(maintenanceFee);
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
