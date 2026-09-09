package com.dday.domain.unexpectedincome.dto.response;

import com.dday.domain.income.entity.RecurringIncome;
import com.dday.domain.mydata.entity.FinancialTransaction;
import com.dday.domain.unexpectedincome.dto.UnexpectedIncomeType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UnexpectedIncomeResponse {

    private Long transactionId;

    private Long amount;

    /**
     * 화면에 표시할 입금주명.
     */
    private String senderName;

    private LocalDateTime transactionAt;

    /**
     * NEW_INCOME
     * RECURRING_LIKELY
     * RECURRING_OVER
     */
    private UnexpectedIncomeType type;

    /**
     * 고정수입 후보가 존재할 때만 값이 들어간다.
     */
    private Long recurringIncomeId;

    private String recurringIncomeName;

    private Long expectedAmount;

    private String depositTiming;

    /**
     * RECURRING_OVER일 때
     * 실제 입금액 - 등록 고정수입 금액.
     */
    private Long excessAmount;

    public static UnexpectedIncomeResponse newIncome(
            FinancialTransaction transaction
    ) {
        return UnexpectedIncomeResponse.builder()
                .transactionId(
                        transaction.getFinancialTransactionId()
                )
                .amount(
                        transaction.getAmount()
                )
                .senderName(
                        resolveSenderName(transaction)
                )
                .transactionAt(
                        transaction.getTransactionAt()
                )
                .type(
                        UnexpectedIncomeType.NEW_INCOME
                )
                .excessAmount(0L)
                .build();
    }

    public static UnexpectedIncomeResponse recurringLikely(
            FinancialTransaction transaction,
            RecurringIncome recurringIncome
    ) {
        return UnexpectedIncomeResponse.builder()
                .transactionId(
                        transaction.getFinancialTransactionId()
                )
                .amount(
                        transaction.getAmount()
                )
                .senderName(
                        resolveSenderName(transaction)
                )
                .transactionAt(
                        transaction.getTransactionAt()
                )
                .type(
                        UnexpectedIncomeType.RECURRING_LIKELY
                )
                .recurringIncomeId(
                        recurringIncome.getRecurringIncomeId()
                )
                .recurringIncomeName(
                        recurringIncome.getIncomeName()
                )
                .expectedAmount(
                        recurringIncome.getExpectedAmount()
                )
                .depositTiming(
                        recurringIncome.getDepositTiming()
                )
                .excessAmount(0L)
                .build();
    }

    public static UnexpectedIncomeResponse recurringOver(
            FinancialTransaction transaction,
            RecurringIncome recurringIncome
    ) {
        long excessAmount =
                transaction.getAmount()
                        - recurringIncome.getExpectedAmount();

        return UnexpectedIncomeResponse.builder()
                .transactionId(
                        transaction.getFinancialTransactionId()
                )
                .amount(
                        transaction.getAmount()
                )
                .senderName(
                        resolveSenderName(transaction)
                )
                .transactionAt(
                        transaction.getTransactionAt()
                )
                .type(
                        UnexpectedIncomeType.RECURRING_OVER
                )
                .recurringIncomeId(
                        recurringIncome.getRecurringIncomeId()
                )
                .recurringIncomeName(
                        recurringIncome.getIncomeName()
                )
                .expectedAmount(
                        recurringIncome.getExpectedAmount()
                )
                .depositTiming(
                        recurringIncome.getDepositTiming()
                )
                .excessAmount(
                        excessAmount
                )
                .build();
    }

    /**
     * 현재 FinancialTransaction에는 별도의
     * "입금주명" 컬럼이 없으므로
     *
     * merchantName → transMemo 순으로 화면에 사용한다.
     */
    private static String resolveSenderName(
            FinancialTransaction transaction
    ) {
        if (transaction.getMerchantName() != null
                && !transaction.getMerchantName().isBlank()) {

            return transaction.getMerchantName();
        }

        if (transaction.getTransMemo() != null
                && !transaction.getTransMemo().isBlank()) {

            return transaction.getTransMemo();
        }

        return "알 수 없는 입금자";
    }
}