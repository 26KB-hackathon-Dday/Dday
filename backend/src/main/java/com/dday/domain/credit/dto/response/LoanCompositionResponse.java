package com.dday.domain.credit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 대출 구성 응답 — 몇 건을 어디서 빌렸는지.
 *
 * <p>{@link #loanCount}·{@link #totalBalance}는 <b>모든 대출</b>을 센다. 반면
 * {@link #sectors}에는 권역을 아는 대출만 담긴다 — 모르는 기관 코드를 제1금융권으로
 * 넘겨짚으면 안내가 실제보다 후해지기 때문이다. 그래서 둘의 합이 다를 수 있다.
 *
 * <p>대출이 없으면 404가 아니라 전부 0인 빈 응답을 준다.
 */
@Getter
@Builder
@AllArgsConstructor
public class LoanCompositionResponse {

    private final int loanCount;

    /** 남은 원금 합계(원). */
    private final long totalBalance;

    /** 제1금융권 → 제2금융권 → 대부업 순. 해당 대출이 없는 권역은 빠진다. */
    private final List<LoanSectorResponse> sectors;

    /** 잔액이 큰 순. */
    private final List<LoanItemResponse> loans;

    public static LoanCompositionResponse empty() {
        return LoanCompositionResponse.builder()
                .sectors(List.of())
                .loans(List.of())
                .build();
    }
}
