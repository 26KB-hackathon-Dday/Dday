package com.dday.domain.mydata.dto.response;

import com.dday.domain.mydata.entity.CardType;
import com.dday.domain.mydata.entity.UserCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 마이페이지 금융정보 화면에 뜨는 연동 카드 한 건. */
@Getter
@Builder
@AllArgsConstructor
public class UserCardResponse {

    private final Long cardId;
    private final String orgCode;
    private final String cardName;
    private final CardType cardType;
    private final Long creditLimit;
    private final boolean selected;
    private final boolean active;
    private final LocalDateTime lastSyncedAt;

    public static UserCardResponse from(UserCard card) {
        return UserCardResponse.builder()
                .cardId(card.getCardId())
                .orgCode(card.getOrgCode())
                .cardName(card.getCardName())
                .cardType(card.getCardType())
                .creditLimit(card.getCreditLimit())
                .selected(card.isSelected())
                .active(card.isActive())
                .lastSyncedAt(card.getLastSyncedAt())
                .build();
    }
}
