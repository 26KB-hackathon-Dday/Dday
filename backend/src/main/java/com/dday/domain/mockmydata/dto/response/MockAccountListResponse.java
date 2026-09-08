package com.dday.domain.mockmydata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MockAccountListResponse {

    private List<MockAccountResponse> accounts;
}
