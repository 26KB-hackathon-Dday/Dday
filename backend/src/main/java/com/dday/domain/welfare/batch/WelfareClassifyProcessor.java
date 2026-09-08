package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.collector.Classification;
import com.dday.domain.welfare.collector.YouthClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * 항목마다 룰을 돌린다. 저장 대상이 아니면 {@code null}을 반환해 Writer로 넘기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareClassifyProcessor implements ItemProcessor<WelfareListItem, ClassifiedProgram> {

    private final YouthClassifier classifier;

    @Override
    @Nullable
    public ClassifiedProgram process(WelfareListItem item) {
        Classification result = classifier.classify(item);
        log.debug("판정 {} [{}] {} — {}", item.getServId(), result.disposition(), item.getServNm(), result.trace());
        return result.stored() ? new ClassifiedProgram(item, result) : null;
    }
}
