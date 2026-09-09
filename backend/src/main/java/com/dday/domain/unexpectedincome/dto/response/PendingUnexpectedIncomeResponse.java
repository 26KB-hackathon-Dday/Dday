package com.dday.domain.unexpectedincome.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PendingUnexpectedIncomeResponse {

    /**
     * 아직 처리하지 않은 입금 건수.
     */
    private int count;

    /**
     * 아직 처리하지 않은 입금의 총액.
     */
    private long totalAmount;

    /**
     * 처리해야 할 입금 목록.
     * 오래된 입금부터 정렬된다.
     */
    private List<UnexpectedIncomeResponse> incomes;

    public static PendingUnexpectedIncomeResponse of(
            List<UnexpectedIncomeResponse> incomes
    ) {
        long totalAmount = incomes.stream()
                .mapToLong(UnexpectedIncomeResponse::getAmount)
                .sum();

        return PendingUnexpectedIncomeResponse.builder()
                .count(incomes.size())
                .totalAmount(totalAmount)
                .incomes(incomes)
                .build();
    }
}