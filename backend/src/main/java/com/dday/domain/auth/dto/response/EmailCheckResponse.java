package com.dday.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class EmailCheckResponse {

    /** 사용 가능하면 true. 이미 가입된 이메일이면 false다(에러가 아니라 정상 응답). */
    private final boolean available;
}
