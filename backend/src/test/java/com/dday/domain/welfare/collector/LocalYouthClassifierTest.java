package com.dday.domain.welfare.collector;

import com.dday.domain.welfare.client.WelfareXml;
import com.dday.domain.welfare.client.dto.LcgvWelfareListResponse;
import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.entity.YouthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 지자체 목록(list-local.xml)을 좁게(searchWrd=자립준비청년) 긁으면, 변환 후 CENTRAL과 같은
 * {@link YouthClassifier}를 태워도 전부 STRONG_YOUTH로 수렴한다 — NOTES.md §11-5.
 */
class LocalYouthClassifierTest {

    private final YouthClassifier classifier = new YouthClassifier();
    private List<WelfareListItem> items;

    @BeforeEach
    void loadRealSample() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/welfare/list-local.xml")) {
            String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            LcgvWelfareListResponse response = WelfareXml.readLcgvListResponse(xml);
            items = response.servListOrEmpty().stream()
                    .map(com.dday.domain.welfare.client.dto.LcgvWelfareListItem::toCommon)
                    .toList();
        }
    }

    @Test
    void 세_건_모두_Rule1으로_STRONG_YOUTH() {
        assertThat(items).hasSize(3);
        items.forEach(item -> {
            Classification c = classifier.classify(item);
            assertThat(c.stored())
                    .as("servId=%s (%s)", item.getServId(), item.getServNm())
                    .isTrue();
            assertThat(c.status()).isEqualTo(YouthStatus.STRONG_YOUTH);
            assertThat(c.trace()).isEqualTo("R1");
        });
    }
}
