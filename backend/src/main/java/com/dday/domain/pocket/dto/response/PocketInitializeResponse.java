package com.dday.domain.pocket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 기본 포켓 초기화에서 실제 생성된 수와 초기화 후 전체 수를 반환한다. */
@Getter
@Builder
@AllArgsConstructor
public class PocketInitializeResponse {

    /** 이번 호출이 새로 삽입한 포켓 수. 이미 모두 존재하면 0이다. */
    private int createdCount;

    /** 초기화 완료 후 보장되는 기본 포켓 유형의 총개수. */
    private int totalCount;
}
