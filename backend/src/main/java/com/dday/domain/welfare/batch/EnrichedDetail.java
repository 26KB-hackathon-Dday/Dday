package com.dday.domain.welfare.batch;

import java.math.BigDecimal;

/**
 * 상세보강 Processor → Writer로 넘기는 묶음. 상세 파싱이 일부 실패해도(금액 못 찾음) rawXml은
 * 넘겨서 {@code raw_detail_xml}을 채운다 — 다음 실행에서 다시 안 부르게.
 */
public record EnrichedDetail(
        String servId,
        BigDecimal supportAmount,
        Integer supportDurationMonths,
        String crtrYr,
        String targetDescription,
        String applyChannelName,
        String applyChannelUrl,
        String applyChannelPhone,
        String rawDetailXml) {
}
