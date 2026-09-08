package com.dday.domain.onboarding.dto.request;

import com.dday.domain.income.entity.IncomeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class IncomeRequest {

    @NotBlank(message = "수입 이름을 입력해주세요.")
    @Size(max = 50, message = "수입 이름은 50자 이하로 입력해주세요.")
    private String name;

    /** SALARY · ALLOWANCE · SETTLEMENT_FUND · DIDIM_SEED · ETC */
    @NotNull(message = "수입 종류를 선택해주세요.")
    private IncomeType incomeType;

    @NotNull(message = "예상 금액을 입력해주세요.")
    @Positive(message = "예상 금액은 0원보다 커야 합니다.")
    private Long amount;

    /** "매월 25일"처럼 사람이 적는 입금 시기. 선택 입력이다. */
    @Size(max = 50, message = "입금 시기는 50자 이하로 입력해주세요.")
    private String paymentTiming;
}
