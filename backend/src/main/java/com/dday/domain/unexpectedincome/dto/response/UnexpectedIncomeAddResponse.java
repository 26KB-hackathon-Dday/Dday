package com.dday.domain.unexpectedincome.dto.response;

import com.dday.domain.budget.entity.MonthlyPocketBudget;
import com.dday.domain.pocket.entity.PocketType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class UnexpectedIncomeAddResponse {

    private Long transactionId;

    private LocalDate budgetMonth;

    /**
     * 실제 입금액.
     */
    private Long incomeAmount;

    /**
     * 실제 입금액 중 이번 달 예산에 반영한 금액.
     */
    private Long addedAmount;

    /**
     * 변경 후 이번 달 총예산.
     */
    private Long totalBudgetAmount;

    private List<PocketBudget> pockets;

    @Getter
    @Builder
    public static class PocketBudget {

        private PocketType pocketType;

        private String pocketName;

        /**
         * 변경 후 해당 포켓의 예산.
         */
        private Long targetAmount;

        public static PocketBudget from(
                MonthlyPocketBudget monthlyPocketBudget
        ) {
            return PocketBudget.builder()
                    .pocketType(
                            monthlyPocketBudget
                                    .getPocket()
                                    .getPocketType()
                    )
                    .pocketName(
                            monthlyPocketBudget
                                    .getPocket()
                                    .getPocketName()
                    )
                    .targetAmount(
                            monthlyPocketBudget.getTargetAmount()
                    )
                    .build();
        }
    }
}