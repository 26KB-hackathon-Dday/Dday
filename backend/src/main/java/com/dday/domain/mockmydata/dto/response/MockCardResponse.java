package com.dday.domain.mockmydata.dto.response;

import com.dday.domain.mockmydata.entity.MockCardType;
import com.dday.domain.mockmydata.entity.MockMydataCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MockCardResponse {

    private String cardId;
    private String orgCode;
    private String cardName;
    private MockCardType cardType;

    /** 카드 한도(원). 체크·선불카드는 {@code null}이다. */
    private Long creditLimit;

    public static MockCardResponse from(MockMydataCard card) {
        return MockCardResponse.builder()
                .cardId(card.getExternalCardId())
                .orgCode(card.getOrgCode())
                .cardName(card.getCardName())
                .cardType(card.getCardType())
                .creditLimit(card.getCreditLimit())
                .build();
    }
}
