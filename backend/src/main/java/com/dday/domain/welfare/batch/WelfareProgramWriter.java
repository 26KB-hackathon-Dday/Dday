package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.entity.WelfareProgram;
import com.dday.domain.welfare.repository.WelfareProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * {@code serv_id} 기준 upsert.
 *
 * <p>{@code collectedAt}은 스텝 실행마다 한 번 잡은 시각으로 모든 행에 같은 값을 넣는다
 * ({@code @StepScope} 인스턴스 필드). 그래야 "이번 실행에서 빠진 후보"를
 * {@code collected_at < :runAt}로 정확히 가를 수 있다.
 *
 * <p>쓰기는 스텝 트랜잭션 안에서 일어나므로, 기존 행을 조회해 필드만 바꾸면 커밋 시 자동 반영된다.
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class WelfareProgramWriter implements ItemWriter<ClassifiedProgram> {

    private final WelfareProgramRepository repository;
    private final LocalDateTime runAt = LocalDateTime.now();

    @Override
    public void write(Chunk<? extends ClassifiedProgram> chunk) {
        for (ClassifiedProgram classified : chunk) {
            String servId = classified.item().getServId();
            repository.findByServId(servId).ifPresentOrElse(
                    existing -> existing.applyCollection(classified.item(), classified.classification(), runAt),
                    () -> repository.save(WelfareProgram.fromCollection(
                            classified.item(), classified.classification(), runAt))
            );
        }
        log.debug("복지서비스 후보 {}건 저장", chunk.size());
    }
}
