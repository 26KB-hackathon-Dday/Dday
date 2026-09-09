package com.dday.domain.mydata.dto.response;

import com.dday.domain.mydata.entity.UserCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class UserCardListResponse {

    private final List<UserCardResponse> cards;

    public static UserCardListResponse of(List<UserCard> cards) {
        return UserCardListResponse.builder()
                .cards(cards.stream().map(UserCardResponse::from).toList())
                .build();
    }
}
