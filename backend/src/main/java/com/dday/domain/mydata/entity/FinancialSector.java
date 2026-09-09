package com.dday.domain.mydata.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 금융기관이 속한 권역.
 *
 * <p>신용점수는 <b>얼마를 빌렸는지보다 어디서 빌렸는지</b>에 더 민감하다. 같은 금액이라도
 * 제2금융권·대부업 대출이 있으면 점수가 더 깎인다. 그래서 기관 이름과 함께 권역을 들고 있어야
 * "은행 1건, 캐피탈 1건"을 구분해 보여줄 수 있다.
 *
 * <p>{@link #label}은 화면에 그대로 쓰는 문구다 (frontend/AGENTS.md).
 */
@Getter
@RequiredArgsConstructor
public enum FinancialSector {

    BANK("제1금융권"),
    NON_BANK("제2금융권"),
    LOAN_COMPANY("대부업");

    private final String label;
}
