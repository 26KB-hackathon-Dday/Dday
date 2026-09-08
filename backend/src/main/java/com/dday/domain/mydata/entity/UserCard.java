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
 * 마이데이터로 연동한 사용자 카드.
 *
 * <p>카드번호를 저장하지 않는다. 마이데이터가 주는 식별값({@link #cardIdentifier})만 들고
 * 있으면 거래를 잇는 데 충분하고, 실제 번호는 유출됐을 때 피해가 크기 때문이다.
 *
 * <p>{@code selected}·{@code active}의 구분은 {@link UserAccount}와 같다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_card",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_card",
                columnNames = {"user_id", "org_code", "card_identifier"}
        ),
        indexes = @Index(name = "idx_user_card_selected", columnList = "user_id, is_selected")
)
public class UserCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long cardId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "org_code", nullable = false, length = 20)
    private String orgCode;

    /** 마이데이터가 내려주는 카드 식별값. 실제 카드번호가 아니다. */
    @Column(name = "card_identifier", nullable = false, length = 100)
    private String cardIdentifier;

    @Column(name = "card_name", length = 100)
    private String cardName;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private CardType cardType;

    @Column(name = "is_selected", nullable = false)
    private boolean selected;

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
    private UserCard(User user, String orgCode, String cardIdentifier, String cardName,
                     CardType cardType) {
        this.user = user;
        this.orgCode = orgCode;
        this.cardIdentifier = cardIdentifier;
        this.cardName = cardName;
        this.cardType = cardType;
        this.selected = false;
        this.active = true;
    }

    public void sync(LocalDateTime syncedAt) {
        this.lastSyncedAt = syncedAt;
    }

    public void select(boolean selected) {
        this.selected = selected;
    }

    public void deactivate() {
        this.active = false;
    }
}
