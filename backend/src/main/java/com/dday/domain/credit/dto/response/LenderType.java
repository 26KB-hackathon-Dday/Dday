package com.dday.domain.credit.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 예상 금리를 보여주는 금융권 구분.
 *
 * <p>회사 하나가 아니라 <b>업권 평균</b>이다. 어느 한 곳을 고르면 그 회사의 특이값
 * (하나카드는 900점 초과가 19.51%인데 601~700점이 12.00%다)이 그대로 화면에 나온다.
 *
 * <p>{@link #label}은 화면에 그대로 쓰는 문구다. 프론트에서 {@code BANK → 은행}을 다시
 * 만들면 양쪽에서 관리하게 되므로 정본을 여기 둔다 (frontend/AGENTS.md).
 *
 * <p>선언 순서가 화면 표시 순서다 — 금리가 낮은 쪽부터.
 */
@Getter
@RequiredArgsConstructor
public enum LenderType {

    BANK("은행"),
    CAPITAL("캐피탈"),
    CARD("카드사");

    private final String label;
}
