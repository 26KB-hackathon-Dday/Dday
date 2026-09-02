package com.dday.domain.welfare.collector;

import com.dday.domain.welfare.client.WelfareXml;
import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.client.dto.WelfareListResponse;
import com.dday.domain.welfare.collector.Classification.Disposition;
import com.dday.domain.welfare.entity.YouthStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code lifeArray=004 & searchWrd=청년}로 나오는 중앙부처 복지서비스 <b>28건 전수</b>를 분류하고
 * 결과를 표로 출력한다. 룰 변경 시 전체 그림이 어떻게 바뀌는지 눈으로 본다.
 */
class YouthClassifierFullSetTest {

    private final YouthClassifier classifier = new YouthClassifier();

    @Test
    void 이십팔건_전수_분류_결과를_출력한다() throws Exception {
        List<WelfareListItem> items = load();
        assertThat(items).hasSize(28);

        Map<String, Integer> tally = new java.util.LinkedHashMap<>();
        tally.put("STRONG_YOUTH", 0);
        tally.put("AUTO_APPROVED", 0);
        tally.put("REVIEW_QUEUE", 0);
        tally.put("REJECTED(score<0)", 0);
        tally.put("DISCARDED(prefilter)", 0);

        System.out.println("\n servId       | 결과            | score | servNm");
        System.out.println("--------------|-----------------|-------|------------------------------------");
        for (WelfareListItem item : items) {
            Classification c = classifier.classify(item);
            String bucket = bucket(c);
            tally.merge(bucket, 1, Integer::sum);
            System.out.printf(" %-12s | %-15s | %5s | %s   [%s]%n",
                    item.getServId(), bucket,
                    c.score() == null ? "-" : c.score(),
                    item.getServNm(),
                    c.trace());
        }

        System.out.println("\n── 집계 ──");
        tally.forEach((k, v) -> System.out.printf("  %-22s %d%n", k, v));
        int stored = tally.get("STRONG_YOUTH") + tally.get("AUTO_APPROVED") + tally.get("REVIEW_QUEUE");
        System.out.printf("  %-22s %d%n", "저장 합계", stored);
        System.out.printf("  %-22s %d%n", "미저장 합계", 28 - stored);

        // Rule 6(servNm "청년" +2) 반영 후 기대값
        assertThat(tally.get("STRONG_YOUTH")).isEqualTo(3);
        assertThat(tally.get("AUTO_APPROVED")).isEqualTo(8);
        assertThat(tally.get("REVIEW_QUEUE")).isEqualTo(4);
        assertThat(tally.get("REJECTED(score<0)")).isEqualTo(4);
        assertThat(tally.get("DISCARDED(prefilter)")).isEqualTo(9);
        assertThat(stored).isEqualTo(15);
    }

    @Test
    void 청년창업농장학금은_servNm의_청년_덕에_검토큐로_구제된다() throws Exception {
        WelfareListItem item = load().stream()
                .filter(i -> i.getServId().equals("WLF00000812"))
                .findFirst().orElseThrow();
        Classification c = classifier.classify(item);
        // R3-2(청년,중장년,노년) + R6+2(servNm "청년창업농장학금") = 0 → 검토 큐
        assertThat(c.disposition()).isEqualTo(Disposition.STORED);
        assertThat(c.status()).isEqualTo(YouthStatus.REVIEW_QUEUE);
        assertThat(c.score()).isZero();
    }

    @Test
    void 일상돌봄은_가족돌봄청년_대상이라도_그대로_제외된다() throws Exception {
        // 자립준비청년은 가족이 없다 — 가족돌봄청년은 이 서비스의 대상 모집단이 아니다.
        WelfareListItem item = load().stream()
                .filter(i -> i.getServId().equals("WLF00005411"))
                .findFirst().orElseThrow();
        assertThat(classifier.classify(item).disposition()).isEqualTo(Disposition.REJECTED);
    }

    private String bucket(Classification c) {
        if (c.disposition() == Disposition.DISCARDED) {
            return "DISCARDED(prefilter)";
        }
        if (c.disposition() == Disposition.REJECTED) {
            return "REJECTED(score<0)";
        }
        return c.status() == YouthStatus.STRONG_YOUTH ? "STRONG_YOUTH"
                : c.status() == YouthStatus.AUTO_APPROVED ? "AUTO_APPROVED"
                : "REVIEW_QUEUE";
    }

    private List<WelfareListItem> load() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/welfare/list-central-all.xml")) {
            String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            WelfareListResponse response = WelfareXml.readListResponse(xml);
            return response.servListOrEmpty();
        }
    }
}
