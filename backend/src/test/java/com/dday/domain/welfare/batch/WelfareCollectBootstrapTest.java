package com.dday.domain.welfare.batch;

import com.dday.domain.welfare.entity.ProgramSource;
import com.dday.domain.welfare.repository.WelfareProgramRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WelfareCollectBootstrapTest {

    private final WelfareCollectLauncher launcher = mock(WelfareCollectLauncher.class);
    private final WelfareProgramRepository repository = mock(WelfareProgramRepository.class);
    private final WelfareCollectBootstrap bootstrap = new WelfareCollectBootstrap(launcher, repository);

    @Test
    void 키가_없으면_최초_수집을_건너뛴다() {
        ReflectionTestUtils.setField(bootstrap, "serviceKey", "  ");

        bootstrap.collectOnceIfNeverRun();

        verifyNoInteractions(launcher);
    }

    @Test
    void 이미_자동수집된_행이_있으면_건너뛴다() {
        ReflectionTestUtils.setField(bootstrap, "serviceKey", "some-key");
        when(repository.existsBySource(ProgramSource.API_CANDIDATE)).thenReturn(true);

        bootstrap.collectOnceIfNeverRun();

        verifyNoInteractions(launcher);
    }

    @Test
    void 키가_있고_수집_이력이_없으면_한_번_실행한다() {
        ReflectionTestUtils.setField(bootstrap, "serviceKey", "some-key");
        when(repository.existsBySource(ProgramSource.API_CANDIDATE)).thenReturn(false);

        bootstrap.collectOnceIfNeverRun();

        verify(launcher, timeout(1000)).launch(); // 별도 스레드에서 돈다
    }
}
