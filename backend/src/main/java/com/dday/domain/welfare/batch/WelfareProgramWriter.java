package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.entity.WelfareProgram;
import com.dday.domain.welfare.repository.WelfareProgramRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * {@code serv_id} 기준 upsert.
 *
 * <p>{@code collectedAt}은 <b>잡 파라미터 {@code runAt}</b>(에포크 밀리초, 런처가 넣는다)에서
 * 온다. 한 잡의 모든 스텝(중앙·지자체)이 같은 값을 쓰므로, "이번 실행에서 빠진 후보"를
 * {@code collected_at < :runAt}로 정확히 가를 수 있다. (스텝마다 {@code now()}를 잡으면
 * 스텝 사이 수 초 차로 앞 스텝 행이 통째로 "빠진 것"으로 잡힌다.)
 *
 * <p>쓰기는 스텝 트랜잭션 안에서 일어나므로, 기존 행을 조회해 필드만 바꾸면 커밋 시 자동 반영된다.
 */
@Slf4j
@Component
@StepScope
public class WelfareProgramWriter implements ItemWriter<ClassifiedProgram> {

    private final WelfareProgramRepository repository;
    private final LocalDateTime runAt;

    public WelfareProgramWriter(WelfareProgramRepository repository,
                                @Value("#{jobParameters['runAt']}") Long runAtEpochMillis) {
        this.repository = repository;
        this.runAt = runAtEpochMillis != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(runAtEpochMillis), ZoneId.systemDefault())
                : LocalDateTime.now();
    }

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
