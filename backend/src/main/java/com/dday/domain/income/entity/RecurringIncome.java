package com.dday.domain.income.entity;

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
 * 매달 들어올 것으로 <b>예상되는</b> 수입 한 건. 사용자가 직접 등록한다.
 *
 * <p>실제로 들어온 돈은 {@code FinancialTransaction}이고 이건 그 예고편이다. 마이데이터를
 * 연결하기 전이거나 첫 달이라 거래 이력이 없을 때도 예산 초안을 만들려면 "얼마가 들어올
 * 예정인지"를 알아야 해서 따로 둔다.
 *
 * <p>예산 초안을 만들 때 이 행들이 {@code BudgetDraftFactor}로 옮겨 담긴다 — 거기는 특정 달의
 * 산출 근거 스냅샷이고, 여기는 달과 무관한 회원의 설정이다. 그래서 금액을 고쳐도 지난달
 * 예산의 근거는 바뀌지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "recurring_income",
        indexes = @Index(name = "idx_recurring_income_user", columnList = "user_id")
)
public class RecurringIncome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recurring_income_id")
    private Long recurringIncomeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 화면에 뜨는 이름. 같은 유형이 여럿일 수 있어 "편의점 알바"처럼 구체적으로 적는다. */
    @Column(name = "income_name", nullable = false, length = 50)
    private String incomeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "income_type", nullable = false, length = 30)
    private IncomeType incomeType;

    /**
     * 매달 예상 금액(원). 실제 입금액이 아니라 사용자가 적어둔 예상치다.
     *
     * <p>원 단위 정수라 {@code Long}이다 — {@code budget} 도메인의 금액 컬럼과 같은 타입이라야
     * 예산 초안으로 옮길 때 변환이 끼지 않는다.
     */
    @Column(name = "expected_amount", nullable = false)
    private Long expectedAmount;

    /**
     * "매월 25일", "격주 금요일"처럼 사람이 적는 입금 시기.
     *
     * <p>날짜(int)가 아니라 문자열인 건 급여일이 주말이면 앞당겨지는 등 규칙이 제각각이라
     * 숫자 하나로는 못 담아서다. 알림을 붙이게 되면 그때 파싱 가능한 컬럼을 따로 만든다.
     */
    @Column(name = "deposit_timing", length = 50)
    private String depositTiming;

    /** 보내는 곳 이름(회사·기관). 거래내역의 상대방 이름과 대조하는 데 쓴다. */
    @Column(name = "source_name", length = 100)
    private String sourceName;

    /** 입금되는 계좌번호. 마스킹된 값이 들어올 수 있어 넉넉히 잡는다. */
    @Column(name = "source_account", length = 100)
    private String sourceAccount;

    /**
     * 들어온 거래를 이 수입에 자동으로 이어붙일지. 기본은 꺼짐이다 —
     * 잘못 매칭되면 사용자가 모르는 사이 예산 실적이 틀어지므로 사용자가 켜야 동작한다.
     */
    @Column(name = "auto_match_enabled", nullable = false)
    private boolean autoMatchEnabled;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private RecurringIncome(User user, String incomeName, IncomeType incomeType,
                            Long expectedAmount, String depositTiming, String sourceName,
                            String sourceAccount, boolean autoMatchEnabled) {
        this.user = user;
        this.incomeName = incomeName;
        this.incomeType = incomeType;
        this.expectedAmount = expectedAmount;
        this.depositTiming = depositTiming;
        this.sourceName = sourceName;
        this.sourceAccount = sourceAccount;
        this.autoMatchEnabled = autoMatchEnabled;
    }

    /**
     * 수입 정보 수정. {@code null}인 항목은 "안 바꾼다"는 뜻이라 건너뛴다 —
     * PATCH로 바뀐 필드만 올라오기 때문이다.
     *
     * <p>{@code sourceName}·{@code sourceAccount}·{@code depositTiming}을 지우는 건
     * 이 메서드로 못 한다. 필요해지면 지우기 전용 메서드를 따로 만든다 —
     * "안 보냈다"와 "비워달라"를 한 파라미터로 구분할 수 없다.
     */
    public void update(String incomeName, IncomeType incomeType, Long expectedAmount,
                       String depositTiming, String sourceName, String sourceAccount) {
        if (incomeName != null) {
            this.incomeName = incomeName;
        }
        if (incomeType != null) {
            this.incomeType = incomeType;
        }
        if (expectedAmount != null) {
            this.expectedAmount = expectedAmount;
        }
        if (depositTiming != null) {
            this.depositTiming = depositTiming;
        }
        if (sourceName != null) {
            this.sourceName = sourceName;
        }
        if (sourceAccount != null) {
            this.sourceAccount = sourceAccount;
        }
    }

    /** 자동 매칭 on/off. 켜고 끄는 게 잦아 {@link #update}와 분리한다. */
    public void changeAutoMatch(boolean enabled) {
        this.autoMatchEnabled = enabled;
    }
}
