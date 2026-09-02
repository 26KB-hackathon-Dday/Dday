package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.client.WelfareApiException;
import com.dday.domain.welfare.client.dto.WelfareListItem;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@code welfareCollectJob} — 목록 수집 + 청년 판정 + 저장.
 *
 * <p>이번 PR은 Step 1(목록)만이다. Step 2(REVIEW_QUEUE 상세보강)는 후속
 * (docs/welfare-collector.md §8).
 *
 * <p>청크 10 = 목록 페이지 크기와 맞춘다. API 호출이 실패하면 청크를 3회까지 재시도한다.
 */
@Configuration
@RequiredArgsConstructor
public class WelfareCollectJobConfig {

    public static final String JOB_NAME = "welfareCollectJob";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job welfareCollectJob(Step collectYouthListStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(collectYouthListStep)
                .build();
    }

    @Bean
    public Step collectYouthListStep(WelfareListReader reader,
                                     WelfareClassifyProcessor processor,
                                     WelfareProgramWriter writer) {
        return new StepBuilder("collectYouthListStep", jobRepository)
                .<WelfareListItem, ClassifiedProgram>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .retry(WelfareApiException.class)
                .retryLimit(3)
                .build();
    }
}
