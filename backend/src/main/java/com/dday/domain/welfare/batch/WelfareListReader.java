package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.client.NationalWelfareApiClient;
import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.client.dto.WelfareListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 목록 API를 페이지 단위로 당겨 항목을 하나씩 흘려보낸다.
 *
 * <p>{@code @StepScope}라 스텝 실행마다 새 인스턴스 → 페이지 커서가 자동으로 초기화된다
 * (매월 도는 잡이라 싱글턴이면 상태가 남는다).
 *
 * <p>{@code totalCount}를 채우기 전까지는 첫 페이지를 받아봐야 알 수 있으므로, 버퍼가 비고
 * 아직 다 못 읽었으면 다음 페이지를 받는다.
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class WelfareListReader implements ItemReader<WelfareListItem> {

    private final NationalWelfareApiClient client;

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
            WelfareListResponse page = client.fetchYouthList(nextPage++);
            if (totalCount < 0) {
                totalCount = page.getTotalCount();
                log.info("복지서비스 수집 시작 — 총 {}건", totalCount);
            }
            List<WelfareListItem> items = page.servListOrEmpty();
            if (items.isEmpty()) {
                return null;
            }
            fetched += items.size();
            buffer.addAll(items);
        }
        return buffer.poll();
    }
}
