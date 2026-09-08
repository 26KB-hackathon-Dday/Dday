package com.dday.domain.welfare.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매월 1일 00:00(KST) 수집 잡을 띄운다. 주기는 {@code welfare.collect.cron}.
 *
 * <p>앱 기동 시 자동 실행은 {@code spring.batch.job.enabled=false}로 막혀 있고,
 * 실행 경로는 이 스케줄러(또는 로컬 전용 수동 엔드포인트)뿐이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareCollectScheduler {

    private final WelfareCollectLauncher launcher;

    @Scheduled(cron = "${welfare.collect.cron}", zone = "${welfare.collect.zone:Asia/Seoul}")
    public void collect() {
        log.info("복지서비스 수집 스케줄 실행");
        launcher.launch();
    }
}
