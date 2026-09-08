package com.dday.domain.welfare.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

/**
 * {@code welfareCollectJob}을 실제로 띄우는 지점. 스케줄러와 개발용 수동 엔드포인트가 공유한다.
 *
 * <p>Spring Batch는 파라미터가 같으면 같은 잡 인스턴스로 보고 재실행을 거부하므로,
 * 매 실행에 {@code runAt}(현재 epoch millis)를 넣어 항상 새 인스턴스가 되게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelfareCollectLauncher {

    private final JobLauncher jobLauncher;
    private final Job welfareCollectJob;

    /** 예외를 삼킨다 — 스케줄러 스레드가 죽지 않도록. 실패는 로그로 남긴다. */
    public void launch() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(welfareCollectJob, params);
        } catch (Exception e) {
            log.error("복지서비스 수집 잡 실행 실패", e);
        }
    }
}
