package com.dday.domain.welfare.repository;

import com.dday.domain.welfare.entity.WelfareProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WelfareProgramRepository extends JpaRepository<WelfareProgram, Long> {

    /** upsert 키 조회. 수집 배치가 신규/갱신을 가른다. */
    Optional<WelfareProgram> findByServId(String servId);

    /**
     * 이번 실행에 포함되지 않은 후보 — 마지막 수집 시각이 이번 실행 기준 시각보다 이전인 행.
     * 목록에서 사라졌거나(종료 의심) 룰이 이번엔 걸러낸 것들이다. 지우지는 않는다.
     */
    List<WelfareProgram> findByCollectedAtBefore(LocalDateTime runAt);
}
