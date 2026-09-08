package com.dday.domain.mydata.client.dto;

import com.dday.domain.mydata.entity.CardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 실제 카드번호를 포함하지 않는 외부 카드의 정규화된 정보다. */
@Getter
@Builder
@AllArgsConstructor
public class MydataCardData {
    private String externalCardId;
    private String orgCode;
    private String cardName;
    private CardType cardType;
}
