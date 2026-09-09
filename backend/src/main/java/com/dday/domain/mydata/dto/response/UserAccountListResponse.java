package com.dday.domain.mydata.dto.response;

import com.dday.domain.mydata.entity.UserAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class UserAccountListResponse {

    private final List<UserAccountResponse> accounts;

    public static UserAccountListResponse of(List<UserAccount> accounts) {
        return UserAccountListResponse.builder()
                .accounts(accounts.stream().map(UserAccountResponse::from).toList())
                .build();
    }
}
