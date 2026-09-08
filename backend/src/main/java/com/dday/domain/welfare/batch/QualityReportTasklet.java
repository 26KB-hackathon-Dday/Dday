package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.collector.validation.WelfareIssue;
import com.dday.domain.welfare.collector.validation.WelfareProgramValidator;
import com.dday.domain.welfare.collector.validation.WelfareQualityReport;
import com.dday.domain.welfare.entity.WelfareProgram;
import com.dday.domain.welfare.repository.WelfareProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 수집 잡의 마지막 스텝 — 저장된 제도 전체를 검증해 {@code curation_status}·{@code curation_issues}에
 * 기록하고, 집계를 로그로 남긴다.
 *
 * <p>큐레이션 값(카테고리·금액·지원대상 등)은 그대로 두고, "이 행을 사람이 봐야 하나"만
 * 판정해 박는다. 리뷰 큐 조회(다음 단계)는 이 컬럼을 읽는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualityReportTasklet implements Tasklet {

    private final WelfareProgramRepository repository;
    private final WelfareProgramValidator validator;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<WelfareProgram> all = repository.findAll();
        for (WelfareProgram program : all) {
            List<WelfareIssue> issues = validator.validate(program);
            program.applyCurationReview(issues);
        }
        repository.saveAll(all);

        WelfareQualityReport report = WelfareQualityReport.of(all, validator);
        if (report.reviewCount() == 0) {
            log.info("수집 품질 리포트 — {}", report.format());
        } else {
            log.warn("수집 품질 리포트\n{}", report.format());
        }
        return RepeatStatus.FINISHED;
    }
}
