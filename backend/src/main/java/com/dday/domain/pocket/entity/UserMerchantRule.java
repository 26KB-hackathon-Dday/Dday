package com.dday.domain.pocket.entity;

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
 * "이 가맹점은 앞으로 이 카테고리·포켓으로" 라는 사용자별 규칙.
 *
 * <p>사용자가 거래 분류를 고치면서 "앞으로도 이렇게"를 선택하면 여기 한 줄이 쌓이고,
 * 이후 같은 가맹점 거래는 자동분류 단계에서 이 규칙이 가장 먼저 적용된다.
 *
 * <p>{@link #merchantKey}가 매칭의 정본이다. 사업자번호가 있으면 {@code REGNO:1234567890},
 * 없으면 정규화한 가맹점명으로 {@code NAME:스타벅스}처럼 만든다 — 접두사를 붙여야
 * "사업자번호 1234"와 "이름이 1234인 가게"가 같은 키로 뭉치지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_merchant_rule",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_merchant_key",
                columnNames = {"user_id", "merchant_key"}
        ),
        indexes = @Index(name = "idx_merchant_rule_user", columnList = "user_id")
)
public class UserMerchantRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_rule_id")
    private Long merchantRuleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** {@code REGNO:사업자번호} 또는 {@code NAME:정규화가맹점명}. 매칭은 이 값으로만 한다. */
    @Column(name = "merchant_key", nullable = false, length = 200)
    private String merchantKey;

    /** 사람이 읽으려고 남기는 원본값. 매칭에는 쓰지 않는다 — 그건 merchantKey 몫이다. */
    @Column(name = "merchant_regno", length = 30)
    private String merchantRegno;

    @Column(name = "merchant_name", length = 150)
    private String merchantName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pocket_id", nullable = false)
    private Pocket pocket;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private UserMerchantRule(User user, String merchantKey, String merchantRegno,
                             String merchantName, Category category, Pocket pocket) {
        this.user = user;
        this.merchantKey = merchantKey;
        this.merchantRegno = merchantRegno;
        this.merchantName = merchantName;
        this.category = category;
        this.pocket = pocket;
    }

    /** 같은 가맹점을 다시 다르게 분류하면 규칙을 새로 만들지 않고 덮어쓴다 (키가 유일하다). */
    public void redirectTo(Category category, Pocket pocket) {
        this.category = category;
        this.pocket = pocket;
    }
}
