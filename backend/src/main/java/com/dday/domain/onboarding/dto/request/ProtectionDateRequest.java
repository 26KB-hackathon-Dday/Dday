package com.dday.domain.onboarding.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ProtectionDateRequest {

    /**
     * 보호종료일 (YYYY-MM-DD).
     *
     * <p>미래 날짜를 막지 않는다 — 아직 보호 중인 회원이 "예정일"을 넣는 게 정상 흐름이다.
     */
    @NotNull(message = "보호종료일을 입력해주세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate protectionEndDate;
}
