package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.entity.ProgramSource;
import com.dday.domain.welfare.repository.WelfareProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 최초 1회 수집. {@link WelfareCollectScheduler}는 매월 1일에만 돌아서, 배포 직후엔
 * 지원금 화면이 비어 있다({@code DEMO-*} 시드만 있는 상태). 그래서 기동 시 자동 수집 이력이
 * 하나도 없으면(= 한 번도 안 돌았으면) 한 번 돌린다.
 *
 * <ul>
 *   <li>{@code WELFARE_API_KEY}가 없으면 건너뛴다 — 매 기동마다 실패 로그를 남기지 않기 위해서.
 *       CI·로컬 기본 상태다.</li>
 *   <li>이미 수집된 행({@link ProgramSource#API_CANDIDATE})이 있으면 건너뛴다 —
 *       재배포마다 외부 API를 때리지 않는다.</li>
 * </ul>
 *
 * <p>기동을 막지 않도록 별도 스레드에서 돌린다. 실패해도 {@link WelfareCollectLauncher}가
 * 예외를 삼키므로, 다음 기동 때 이력이 여전히 없어 자동으로 재시도된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareCollectBootstrap {

    private final WelfareCollectLauncher launcher;
    private final WelfareProgramRepository welfareProgramRepository;

    @Value("${welfare.api.service-key:}")
    private String serviceKey;

    @EventListener(ApplicationReadyEvent.class)
    public void collectOnceIfNeverRun() {
        if (serviceKey == null || serviceKey.isBlank()) {
            log.info("WELFARE_API_KEY 미설정 — 최초 수집 건너뜀");
            return;
        }
        if (welfareProgramRepository.existsBySource(ProgramSource.API_CANDIDATE)) {
            return;
        }

        Thread worker = new Thread(() -> {
            log.info("복지서비스 수집 이력이 없어 최초 1회 수집을 실행한다");
            launcher.launch();
        }, "welfare-first-collect");
        worker.setDaemon(true);
        worker.start();
    }
}
