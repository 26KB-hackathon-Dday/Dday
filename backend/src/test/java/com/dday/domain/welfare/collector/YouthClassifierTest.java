package com.dday.domain.welfare.collector;

import com.dday.domain.welfare.client.WelfareXml;
import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.client.dto.WelfareListResponse;
import com.dday.domain.welfare.collector.Classification.Disposition;
import com.dday.domain.welfare.entity.YouthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 사람이 채팅으로 하던 판정을 코드가 그대로 재현하는지 — docs/welfare-api/NOTES.md §7-4의 10건.
 * <b>이게 이 수집기의 핵심 회귀 테스트다.</b> 룰 가중치를 바꾸면 여기부터 깨진다.
 */
class YouthClassifierTest {

    private final YouthClassifier classifier = new YouthClassifier();
    private Map<String, WelfareListItem> items;

    @BeforeEach
    void loadRealSample() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/welfare/list-central.xml")) {
            String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            WelfareListResponse response = WelfareXml.readListResponse(xml);
            items = response.servListOrEmpty().stream()
                    .collect(Collectors.toMap(WelfareListItem::getServId, Function.identity()));
        }
    }

    private Classification classify(String servId) {
        return classifier.classify(items.get(servId));
    }

    @Test
    void 청년월세_자동승인() {
        Classification c = classify("WLF00004661");
        assertThat(c.disposition()).isEqualTo(Disposition.STORED);
        assertThat(c.status()).isEqualTo(YouthStatus.AUTO_APPROVED);
        assertThat(c.score()).isEqualTo(7); // R2+2, R3+3, R6+2(servNm "청년월세")
    }

    @Test
    void 청년내일저축계좌_자동승인() {
        Classification c = classify("WLF00000060");
        assertThat(c.status()).isEqualTo(YouthStatus.AUTO_APPROVED);
        assertThat(c.score()).isEqualTo(5); // R3+3, R6+2(servNm "청년내일저축계좌")
    }

    @Test
    void 햇살론youth_검토큐_2점_servNm은_youth라_R6없음() {
        Classification c = classify("WLF00001076");
        assertThat(c.status()).isEqualTo(YouthStatus.REVIEW_QUEUE);
        assertThat(c.score()).isEqualTo(2); // R3+3, R5-1 (대학생, 청년 나열). servNm "햇살론youth"는 "청년" 아님
    }

    @Test
    void 학자금대출_검토큐_2점_요약에_청년없어도_소관부서로_통과() {
        Classification c = classify("WLF00003277");
        assertThat(c.disposition()).isEqualTo(Disposition.STORED);
        assertThat(c.status()).isEqualTo(YouthStatus.REVIEW_QUEUE);
        assertThat(c.score()).isEqualTo(2); // R2+2 (청년장학지원과)
    }

    @Test
    void 행복주택_자동제외_음수() {
        Classification c = classify("WLF00004649");
        assertThat(c.disposition()).isEqualTo(Disposition.REJECTED);
        assertThat(c.stored()).isFalse();
        assertThat(c.score()).isEqualTo(-6); // R3-2, R4-3, R5-1
    }

    @Test
    void 국민취업지원제도_자동제외_음수() {
        Classification c = classify("WLF00003245");
        assertThat(c.disposition()).isEqualTo(Disposition.REJECTED);
        assertThat(c.score()).isEqualTo(-3); // R3-2, R5-1
    }

    @Test
    void 통합공공임대_사전필터_탈락_요약에_청년이_없다() {
        Classification c = classify("WLF00004663");
        assertThat(c.disposition()).isEqualTo(Disposition.DISCARDED);
    }

    @Test
    void 기존주택_매입임대_사전필터_탈락() {
        assertThat(classify("WLF00000062").disposition()).isEqualTo(Disposition.DISCARDED);
    }

    @Test
    void 예술활동준비금_사전필터_탈락_생애주기에만_청년() {
        assertThat(classify("WLF00003199").disposition()).isEqualTo(Disposition.DISCARDED);
    }

    @Test
    void 주거안정_월세대출_사전필터_탈락() {
        assertThat(classify("WLF00001063").disposition()).isEqualTo(Disposition.DISCARDED);
    }

    @Test
    void 저장되는_건_네_건뿐이다() {
        long stored = items.keySet().stream()
                .map(this::classify)
                .filter(Classification::stored)
                .count();
        assertThat(stored).isEqualTo(4); // 청년월세, 청년내일저축계좌, 햇살론youth, 학자금대출
    }
}
