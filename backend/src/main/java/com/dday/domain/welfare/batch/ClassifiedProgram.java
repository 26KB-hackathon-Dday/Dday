package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.collector.Classification;

/**
 * Processor → Writer로 넘기는 묶음: 원본 항목 + 판정 결과.
 * 저장 대상({@link Classification#stored()})만 여기까지 온다.
 */
public record ClassifiedProgram(WelfareListItem item, Classification classification) {
}
