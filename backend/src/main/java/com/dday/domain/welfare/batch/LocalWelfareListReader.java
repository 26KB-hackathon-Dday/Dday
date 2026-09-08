package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.client.LocalWelfareApiClient;
import com.dday.domain.welfare.client.dto.LcgvWelfareListItem;
import com.dday.domain.welfare.client.dto.LcgvWelfareListResponse;
import com.dday.domain.welfare.client.dto.WelfareListItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 지자체 목록 API를 페이지 단위로 당겨, 공통 {@link WelfareListItem}으로 변환해 하나씩 흘려보낸다.
 *
 * <p>{@link WelfareListReader}(CENTRAL)와 페이징 로직이 같다. 차이는 (1) 지자체 클라이언트를
 * 호출하고 (2) {@link LcgvWelfareListItem#toCommon()}으로 변환한다는 것뿐이다. 변환 후에는
 * CENTRAL과 완전히 같은 Processor·Writer를 탄다.
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class LocalWelfareListReader implements ItemReader<WelfareListItem> {

    private final LocalWelfareApiClient client;

    private final Deque<WelfareListItem> buffer = new ArrayDeque<>();
    private int nextPage = 1;
    private int totalCount = -1;
    private int fetched = 0;

    @Override
    public WelfareListItem read() {
        if (buffer.isEmpty()) {
            if (totalCount >= 0 && fetched >= totalCount) {
                return null;
            }
            LcgvWelfareListResponse page = client.fetchList(nextPage++);
            if (totalCount < 0) {
                totalCount = page.getTotalCount();
                log.info("지자체 복지서비스 수집 시작 — 총 {}건", totalCount);
            }
            List<LcgvWelfareListItem> items = page.servListOrEmpty();
            if (items.isEmpty()) {
                return null;
            }
            fetched += items.size();
            items.forEach(item -> buffer.add(item.toCommon()));
        }
        return buffer.poll();
    }
}
