package com.dday.domain.mydata.dto.response;

import com.dday.domain.mydata.entity.UserAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class UserAccountListResponse {

    private final List<UserAccountResponse> accounts;

    public static UserAccountListResponse of(List<UserAccount> accounts,
                                             Map<Long, Long> monthlyContributions) {
        return UserAccountListResponse.builder()
                .accounts(accounts.stream()
                        .map(account -> UserAccountResponse.from(
                                account,
                                monthlyContributions.getOrDefault(account.getAccountId(), 0L)))
                        .toList())
                .build();
    }
}
