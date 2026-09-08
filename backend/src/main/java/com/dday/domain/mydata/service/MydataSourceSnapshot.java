package com.dday.domain.mydata.service;

import com.dday.domain.mydata.entity.UserAccount;
import com.dday.domain.mydata.entity.UserCard;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/** 외부 식별자와 방금 upsert한 서비스 엔티티를 거래 수집 단계까지 안전하게 전달한다. */
@Getter
@AllArgsConstructor
public class MydataSourceSnapshot {
    private final Map<String, UserAccount> accountsByExternalId;
    private final Map<String, UserCard> cardsByExternalId;
}
