package com.dday.domain.mydata.entity;

import com.dday.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 마이데이터로 연동한 사용자 계좌.
 *
 * <p>{@code (user, orgCode, accountNum)}이 유일하다. 같은 계좌를 다시 당겨도 행이 늘지 않고
 * 잔액만 갱신되도록 하기 위한 것이다.
 *
 * <p>{@link #selected}와 {@link #active}는 다르다. <b>selected</b>는 "이 계좌를 서비스 예산에
 * 쓸지"를 사용자가 고른 값이고, <b>active</b>는 "금융기관에서 아직 살아있는 계좌인지"다.
 * 해지된 계좌를 지우지 않고 active만 내리는 이유는, 지우면 그 계좌에 달린 과거 거래가
 * 통째로 사라져 지난달 결산이 바뀌기 때문이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_account",
                columnNames = {"user_id", "org_code", "account_num"}
        ),
        indexes = @Index(name = "idx_user_account_selected", columnList = "user_id, is_selected")
)
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 금융기관 코드. 계좌번호는 기관 안에서만 유일해서 항상 이 값과 함께 봐야 한다. */
    @Column(name = "org_code", nullable = false, length = 20)
    private String orgCode;

    @Column(name = "account_num", nullable = false, length = 100)
    private String accountNum;

    @Column(name = "account_name", length = 100)
    private String accountName;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    /** 원 단위 정수라 {@code Long}이다. 소수점이 없으니 BigDecimal을 쓸 이유가 없다. */
    @Column(nullable = false)
    private Long balance;

    /** 출금 가능 잔액. 마이너스 통장·예약이체 때문에 잔액과 다를 수 있고, 안 주는 기관도 있다. */
    @Column(name = "available_balance")
    private Long availableBalance;

    /** 서비스 예산 계산에 이 계좌를 넣을지. 사용자가 고른다. */
    @Column(name = "is_selected", nullable = false)
    private boolean selected;

    /** 금융기관 기준으로 살아있는 계좌인지. 해지돼도 행은 남긴다. */
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private UserAccount(User user, String orgCode, String accountNum, String accountName,
                        String productName, AccountType accountType, Long balance,
                        Long availableBalance) {
        this.user = user;
        this.orgCode = orgCode;
        this.accountNum = accountNum;
        this.accountName = accountName;
        this.productName = productName;
        this.accountType = accountType;
        this.balance = balance != null ? balance : 0L;
        this.availableBalance = availableBalance;
        this.selected = false;
        this.active = true;
    }

    /**
     * 외부 계좌의 최신 표시 정보와 잔액을 반영한다.
     *
     * <p>사용자가 정한 {@code selected}는 외부 데이터가 아니므로 절대 덮어쓰지 않는다.
     * 이전 동기화에서 비활성 처리된 계좌가 다시 응답에 나타나면 활성 상태로 복구한다.
     */
    public void sync(String accountName, String productName, AccountType accountType,
                     Long balance, Long availableBalance, LocalDateTime syncedAt) {
        this.accountName = accountName;
        this.productName = productName;
        this.accountType = accountType;
        this.balance = balance;
        this.availableBalance = availableBalance;
        this.lastSyncedAt = syncedAt;
        this.active = true;
    }

    public void select(boolean selected) {
        this.selected = selected;
    }

    /** 금융기관에서 사라진 계좌. 행과 거래는 남기고 표시만 내린다. */
    public void deactivate() {
        this.active = false;
    }
}
