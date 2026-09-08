package com.dday.domain.pocket.dto.request;

import com.dday.domain.pocket.entity.PocketType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/** 사용자가 거래 한 건의 소비 포켓과 카테고리를 직접 확정할 때 사용하는 요청이다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionClassificationRequest {

    @NotNull(message = "포켓 유형을 선택해주세요.")
    private PocketType pocketType;

    @NotNull(message = "카테고리를 선택해주세요.")
    private Long categoryId;

    /** 같은 가맹점의 이후 거래에도 이번 선택을 적용할지 여부다. 생략하면 false다. */
    private boolean applyFutureRule;
}
