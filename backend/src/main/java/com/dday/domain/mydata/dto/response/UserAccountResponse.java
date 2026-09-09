package com.dday.domain.mydata.dto.response;

import com.dday.domain.mydata.entity.AccountType;
import com.dday.domain.mydata.entity.UserAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 마이페이지 금융정보 화면에 뜨는 연동 계좌 한 건. */
@Getter
@Builder
@AllArgsConstructor
public class UserAccountResponse {

    private final Long accountId;
    private final String orgCode;
    private final String accountName;
    private final String productName;
    private final AccountType accountType;
    private final Long balance;
    private final Long availableBalance;
    private final boolean selected;
    private final boolean active;
    private final LocalDateTime lastSyncedAt;

    public static UserAccountResponse from(UserAccount account) {
        return UserAccountResponse.builder()
                .accountId(account.getAccountId())
                .orgCode(account.getOrgCode())
                .accountName(account.getAccountName())
                .productName(account.getProductName())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .availableBalance(account.getAvailableBalance())
                .selected(account.isSelected())
                .active(account.isActive())
                .lastSyncedAt(account.getLastSyncedAt())
                .build();
    }
}
