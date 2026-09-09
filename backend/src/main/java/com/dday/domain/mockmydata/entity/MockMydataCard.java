package com.dday.domain.mockmydata.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Mock 금융기관이 들고 있는 카드.
 *
 * <p>계좌와 달리 {@code (mock_user_id, org_code, …)} 유일 제약이 없다 — 같은 카드사에서
 * 같은 상품을 두 장 발급받는 일이 실제로 있기 때문이다. 중복을 막는 건
 * {@link #externalCardId} 하나뿐이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "mock_mydata_card",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mock_external_card",
                columnNames = "external_card_id"
        ),
        indexes = @Index(name = "idx_mock_card_user", columnList = "mock_user_id")
)
public class MockMydataCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mock_card_id")
    private Long mockCardId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mock_user_id", nullable = false)
    private MockMydataUser mockUser;

    /** Mock API가 밖으로 내보내는 카드 ID. */
    @Column(name = "external_card_id", nullable = false, length = 100)
    private String externalCardId;

    @Column(name = "org_code", nullable = false, length = 20)
    private String orgCode;

    @Column(name = "card_name", length = 100)
    private String cardName;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private MockCardType cardType;

    /** 카드 한도(원). 체크·선불카드는 한도가 없으므로 {@code null}이다. */
    @Column(name = "credit_limit")
    private Long creditLimit;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private MockMydataCard(MockMydataUser mockUser, String externalCardId, String orgCode,
                           String cardName, MockCardType cardType, Long creditLimit) {
        this.mockUser = mockUser;
        this.externalCardId = externalCardId;
        this.orgCode = orgCode;
        this.cardName = cardName;
        this.cardType = cardType;
        this.creditLimit = creditLimit;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
