package com.dday.domain.onboarding.dto.request;

import com.dday.domain.income.entity.IncomeType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 정기수입 수정. PATCH다 — 보내지 않은 필드는 바뀌지 않는다({@link
 * com.dday.domain.income.entity.RecurringIncome#update}가 null을 건너뛴다).
 */
@Getter
@NoArgsConstructor
public class IncomeUpdateRequest {

    @Size(max = 50, message = "수입 이름은 50자 이하로 입력해주세요.")
    private String name;

    /** SALARY · ALLOWANCE · SETTLEMENT_FUND · DIDIM_SEED · ETC */
    private IncomeType incomeType;

    @Positive(message = "예상 금액은 0원보다 커야 합니다.")
    private Long amount;

    @Size(max = 50, message = "입금 시기는 50자 이하로 입력해주세요.")
    private String paymentTiming;
}
