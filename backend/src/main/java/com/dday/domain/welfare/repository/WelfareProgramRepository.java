package com.dday.domain.welfare.repository;

import com.dday.domain.welfare.entity.CurationStatus;
import com.dday.domain.welfare.entity.ProgramSource;
import com.dday.domain.welfare.entity.SupportType;
import com.dday.domain.welfare.entity.WelfareProgram;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WelfareProgramRepository extends JpaRepository<WelfareProgram, Long> {

    /** upsert 키 조회. 수집 배치가 신규/갱신을 가른다. 상세 조회(SUBSIDY-005)도 이걸 쓴다. */
    Optional<WelfareProgram> findByServId(String servId);

    /**
     * 이번 실행에 포함되지 않은 후보 — 마지막 수집 시각이 이번 실행 기준 시각보다 이전인 행.
     * 목록에서 사라졌거나(종료 의심) 룰이 이번엔 걸러낸 것들이다. 지우지는 않는다.
     */
    List<WelfareProgram> findByCollectedAtBefore(LocalDateTime runAt);

    /**
     * 상세보강 대상 — 아직 상세를 안 부른 저장 제도 전부.
     * {@code raw_detail_xml}이 채워지면 다음 실행에서 제외된다(멱등).
     *
     * <p>{@code target_description}·{@code apply_channel_*}는 현금성 여부와 무관하게 필요하다.
     * 금액({@code support_amount}) 파싱만 Processor에서 {@link SupportType#CASH}로 가른다.
     */
    List<WelfareProgram> findByRawDetailXmlIsNull();

    /**
     * 전체 지원제도 검색 (SUBSIDY-009).
     *
     * <p>{@code q}(검색어)와 {@code category}는 서로 독립된 축이다 — 둘 다 주면 AND,
     * 각각 {@code null}/빈 문자열이면 그 조건을 걸지 않는다.
     * 검색 대상은 제도명·소관부처·지원대상 서술.
     *
     * <p>검증에서 걸린 행({@code curation_status = NEEDS_REVIEW})은 결과에서 뺀다 —
     * {@link WelfareProgram#isPubliclyVisible()}와 짝이다.
     */
    @Query("""
            select p from WelfareProgram p
            where (:q is null or :q = ''
                   or lower(p.servNm) like lower(concat('%', :q, '%'))
                   or lower(p.jurMnofNm) like lower(concat('%', :q, '%'))
                   or lower(p.targetDescription) like lower(concat('%', :q, '%')))
              and (:category is null or :category = '' or p.category = :category)
              and (p.source = com.dday.domain.welfare.entity.ProgramSource.MANUAL_CURATION
                   or p.curationStatus is null
                   or p.curationStatus <> com.dday.domain.welfare.entity.CurationStatus.NEEDS_REVIEW)
            """)
    Page<WelfareProgram> search(@Param("q") String q,
                                @Param("category") String category,
                                Pageable pageable);

    /**
     * 리뷰 큐 — 검증에서 걸렸고({@code NEEDS_REVIEW}) 아직 아무도 손 안 댄({@code API_CANDIDATE}) 행.
     * 관리자가 교정 후 {@code source}를 {@code MANUAL_CURATION}으로 바꾸면 큐에서 빠진다.
     */
    List<WelfareProgram> findByCurationStatusAndSourceOrderByServId(CurationStatus curationStatus,
                                                                    ProgramSource source);

    /** 홈 집계 — 자격 있는 제도들을 servId(=programId)로 한 번에. */
    List<WelfareProgram> findByServIdIn(Collection<String> servIds);
}
