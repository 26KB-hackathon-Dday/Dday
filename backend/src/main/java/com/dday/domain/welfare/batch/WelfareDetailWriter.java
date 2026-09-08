package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.repository.WelfareProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * 상세에서 뽑은 값을 해당 행에 반영한다. Reader가 읽은 엔티티는 청크 트랜잭션 밖이라
 * ({@code ListItemReader}) 여기서 {@code servId}로 다시 조회해 영속 상태로 만든 뒤 바꾼다
 * ({@link WelfareProgramWriter}와 같은 방식).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareDetailWriter implements ItemWriter<EnrichedDetail> {

    private final WelfareProgramRepository repository;

    @Override
    public void write(Chunk<? extends EnrichedDetail> chunk) {
        for (EnrichedDetail e : chunk) {
            repository.findByServId(e.servId()).ifPresent(program ->
                    program.applyDetail(
                            e.supportAmount(),
                            e.supportDurationMonths(),
                            e.crtrYr(),
                            e.targetDescription(),
                            e.applyChannelName(),
                            e.applyChannelUrl(),
                            e.applyChannelPhone(),
                            e.rawDetailXml()));
        }
        log.debug("상세보강 {}건 반영", chunk.size());
    }
}
