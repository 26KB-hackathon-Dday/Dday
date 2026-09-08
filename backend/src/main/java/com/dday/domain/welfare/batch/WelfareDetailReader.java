package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.entity.WelfareProgram;
import com.dday.domain.welfare.repository.WelfareProgramRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.stereotype.Component;

/**
 * 상세보강 대상을 스텝 시작 시 한 번에 읽는다 — 아직 상세를 안 부른 저장 제도 전부
 * ({@code raw_detail_xml IS NULL}). 건수가 한 자릿수~십몇 건이라 페이징 없이 리스트로 간다
 * (페이징하면서 {@code raw_detail_xml}을 채우면 오프셋이 밀린다).
 *
 * <p>{@code @StepScope}라 매 실행마다 그 시점의 미보강 목록을 새로 잡는다.
 */
@Slf4j
@Component
@StepScope
public class WelfareDetailReader extends ListItemReader<WelfareProgram> {

    public WelfareDetailReader(WelfareProgramRepository repository) {
        super(repository.findByRawDetailXmlIsNull());
    }
}
