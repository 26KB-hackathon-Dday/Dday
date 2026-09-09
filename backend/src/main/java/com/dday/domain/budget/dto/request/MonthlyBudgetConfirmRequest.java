package com.dday.domain.budget.dto.request;

import com.dday.domain.pocket.entity.PocketType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class MonthlyBudgetConfirmRequest {
    @NotNull(message = "총 예산은 필수입니다.")
    @Min(value = 1, message = "총 예산은 0원보다 커야 합니다.")
    private Long totalBudgetAmount;

    @Valid
    @NotNull(message = "포켓 예산은 필수입니다.")
    @Size(min = 4, max = 4, message = "포켓 예산은 네 개를 입력해야 합니다.")
    private List<PocketAmountRequest> pockets;

    @Getter
    @NoArgsConstructor
    public static class PocketAmountRequest {
        @NotNull(message = "포켓 유형은 필수입니다.")
        private PocketType pocketType;

        @NotNull(message = "포켓 예산은 필수입니다.")
        @Min(value = 0, message = "포켓 예산은 0원 이상이어야 합니다.")
        private Long amount;
    }
}
