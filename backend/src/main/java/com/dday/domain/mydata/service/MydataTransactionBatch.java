package com.dday.domain.mydata.service;

import com.dday.domain.mydata.client.dto.MydataAccountTransactionData;
import com.dday.domain.mydata.client.dto.MydataCardTransactionData;
import com.dday.domain.mydata.entity.UserAccount;
import com.dday.domain.mydata.entity.UserCard;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** 한 금융 원천과 그 원천에서 조회한 거래 목록을 함께 전달하는 내부 작업 단위다. */
public final class MydataTransactionBatch {

    private MydataTransactionBatch() {
    }

    @Getter
    @AllArgsConstructor
    public static class AccountBatch {
        private final UserAccount account;
        private final List<MydataAccountTransactionData> transactions;
    }

    @Getter
    @AllArgsConstructor
    public static class CardBatch {
        private final UserCard card;
        private final List<MydataCardTransactionData> transactions;
    }
}
