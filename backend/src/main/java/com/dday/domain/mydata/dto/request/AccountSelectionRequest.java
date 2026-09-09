package com.dday.domain.mydata.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 계좌를 예산 계산에 포함시킬지. {@code null}을 허용하지 않는다 — on/off 둘 중 하나를 확정해야 한다. */
@Getter
@NoArgsConstructor
public class AccountSelectionRequest {

    @NotNull(message = "선택 여부를 입력해주세요.")
    private Boolean selected;
}
