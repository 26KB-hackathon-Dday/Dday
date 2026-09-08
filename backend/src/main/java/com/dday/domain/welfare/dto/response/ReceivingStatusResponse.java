package com.dday.domain.welfare.dto.response;

import com.dday.domain.welfare.entity.ReceivingStatus;
import com.dday.domain.welfare.entity.UserProgramStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 수급 여부 변경 결과 (SUBSIDY-003). */
@Getter
@AllArgsConstructor
@Builder
public class ReceivingStatusResponse {

    private String programId;
    private ReceivingStatus receivingStatus;
    private LocalDateTime statusUpdatedAt;

    public static ReceivingStatusResponse from(UserProgramStatus s) {
        return ReceivingStatusResponse.builder()
                .programId(s.getId().getProgramId())
                .receivingStatus(s.getReceivingStatus())
                .statusUpdatedAt(s.getStatusUpdatedAt())
                .build();
    }
}
