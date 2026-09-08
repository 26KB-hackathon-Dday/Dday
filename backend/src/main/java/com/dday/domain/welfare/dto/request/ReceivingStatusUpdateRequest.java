package com.dday.domain.welfare.dto.request;

import com.dday.domain.welfare.entity.ReceivingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수급 여부 등록/수정 (SUBSIDY-003).
 *
 * <p>{@code RECEIVING} / {@code NOT_RECEIVING}만 받는다 — {@code LIKELY_RECEIVING}은 시스템 제안값,
 * {@code UNKNOWN}은 초기값이라 사용자가 직접 넘길 이유가 없다.
 */
@Getter
@NoArgsConstructor
public class ReceivingStatusUpdateRequest {

    @NotNull(message = "수급 여부를 선택해주세요.")
    private ReceivingStatus receivingStatus;
}
