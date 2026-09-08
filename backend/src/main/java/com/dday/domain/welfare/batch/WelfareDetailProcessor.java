package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.client.LocalWelfareApiClient;
import com.dday.domain.welfare.client.NationalWelfareApiClient;
import com.dday.domain.welfare.client.dto.WelfareDetailResponse;
import com.dday.domain.welfare.collector.curation.DetailText;
import com.dday.domain.welfare.collector.curation.SupportAmountParser;
import com.dday.domain.welfare.entity.AgencyType;
import com.dday.domain.welfare.entity.SupportType;
import com.dday.domain.welfare.entity.WelfareProgram;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * 저장된 제도마다 상세 API를 호출해 뽑는다:
 * <ul>
 *   <li>{@code tgtrDtlCn}/{@code sprtTrgtCn}(지원대상) → {@code targetDescription} — 전 제도</li>
 *   <li>{@code inqplCtadrList}·{@code inqplHmpgReldList}(문의처·홈페이지) → {@code applyChannel*} — 전 제도</li>
 *   <li>{@code alwServCn}(지원내용) → 금액·개월 — <b>{@link SupportType#CASH}만</b> (대출·서비스 금액은
 *       "지원액"이 아니라 성격이 달라 파싱해도 오해를 부른다)</li>
 * </ul>
 * 중앙/지자체는 상세 엔드포인트가 다르지만 응답 루트({@code <wantedDtl>})는 같다.
 *
 * <p>일부가 비어도 rawXml은 넘긴다 — {@code raw_detail_xml}이 채워져야 다음 실행에서 제외된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareDetailProcessor implements ItemProcessor<WelfareProgram, EnrichedDetail> {

    private final NationalWelfareApiClient centralClient;
    private final LocalWelfareApiClient localClient;

    @Override
    public EnrichedDetail process(WelfareProgram program) {
        WelfareDetailResponse detail = program.getAgencyType() == AgencyType.CENTRAL
                ? centralClient.fetchDetail(program.getServId())
                : localClient.fetchDetail(program.getServId());

        // 금액은 현금성 제도만 뽑는다
        SupportAmountParser.Parsed parsed = program.getSupportType() == SupportType.CASH
                ? SupportAmountParser.parse(detail.getAlwServCn(), program.getSupportAmountType())
                : new SupportAmountParser.Parsed(null, null);
        log.debug("상세보강 {} [{}] 금액={} 개월={} 채널={}", program.getServId(), program.getServNm(),
                parsed.amount(), parsed.months(), detail.channelName());

        return new EnrichedDetail(
                program.getServId(),
                parsed.amount(),
                parsed.months(),
                detail.getCrtrYr(),
                DetailText.cleanTarget(detail.targetDescription()),
                detail.channelName(),
                DetailText.normalizeUrl(detail.channelUrl()),
                detail.channelPhone(),
                detail.getRawXml());
    }
}
