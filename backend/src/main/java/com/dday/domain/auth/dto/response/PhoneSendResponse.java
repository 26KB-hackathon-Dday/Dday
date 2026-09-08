package com.dday.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PhoneSendResponse {

    /** 인증번호 유효시간(초). 프론트가 이 값으로 카운트다운을 그린다. */
    private final int expiresIn;
}
