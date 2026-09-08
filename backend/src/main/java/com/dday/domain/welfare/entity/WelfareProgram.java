package com.dday.domain.welfare.entity;

import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.collector.Classification;
import com.dday.domain.welfare.collector.curation.SupportCycleMapper;
import com.dday.domain.welfare.collector.curation.SupportTypeMapper;
import com.dday.domain.welfare.collector.curation.WelfareCategoryClassifier;
import com.dday.domain.welfare.collector.validation.WelfareIssue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 수집 배치가 만든 복지서비스 후보 하나.
 *
 * <p>외부 API의 {@code servId}가 자연키다. 매월 다시 수집하므로 같은 {@code servId}는
 * 새 행을 만들지 않고 {@link #applyCollection}으로 갱신한다(멱등).
 *
 * <p>{@code createdAt}은 이 후보가 처음 잡힌 시점, {@code collectedAt}은 마지막으로 수집에
 * 포함된 시점이다. 둘의 차이로 "이번 실행에서 빠진 후보"를 찾는다
 * ({@code where collected_at < :thisRun}) — docs/welfare-collector.md §3.
 *
 * <p>Setter는 두지 않는다(AGENTS.md §4). 값 변경은 {@link #applyCollection} 하나로만.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "welfare_program",
        uniqueConstraints = @UniqueConstraint(name = "uk_welfare_program_serv_id", columnNames = "serv_id")
)
public class WelfareProgram {

    /**
     * 내부 기본키. Notion DB 스키마에 맞춰 UUID를 쓴다 — 업무 식별자({@code servId})와 분리.
     * {@code VARCHAR(36)}으로 저장해 CLI/로그에서 바로 읽히게 한다 (BINARY(16) 대신).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, updatable = false, nullable = false)
    private UUID id;

    /** 외부 API 서비스 ID (예: {@code WLF00004661}). upsert 키. */
    @Column(name = "serv_id", nullable = false, length = 20)
    private String servId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AgencyType agencyType;

    // ── 지역 (LOCAL 전용, CENTRAL은 null) — docs/welfare-api/NOTES.md §11-6 ──

    /** 시도명 (예: {@code 서울특별시}). CENTRAL은 {@code null}(전국 시행). */
    @Column(length = 30)
    private String ctpvNm;

    /** 시군구명 (예: {@code 용산구}). 광역 사업·CENTRAL은 {@code null}. */
    @Column(length = 30)
    private String sggNm;

    /** LOCAL 사업담당부서 전체 문자열. CENTRAL은 {@code jurMnofNm}+{@code jurOrgNm}을 쓴다. */
    @Column(length = 255)
    private String bizChrDeptNm;

    /** LOCAL 신청방법 요약 (예: {@code 방문, 전화, 우편}). */
    @Column(length = 100)
    private String aplyMtdNm;

    /** LOCAL 최종수정일 {@code YYYYMMDD}. 종료 감지(후속)용. CENTRAL은 {@code null}. */
    @Column(length = 8)
    private String lastModYmd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgramSource source;

    @Column(length = 255)
    private String servNm;

    /** 서비스 한 줄 요약. 룰 판정(사전필터·Rule 1·Rule 5)의 주 입력이다. */
    @Column(length = 1000)
    private String servDgst;

    /** 소관 부처 (예: 국토교통부). */
    @Column(length = 120)
    private String jurMnofNm;

    /** 소관 부서 (예: 청년주거정책과). Rule 2가 여기서 "청년"을 찾는다. */
    @Column(length = 120)
    private String jurOrgNm;

    /** 생애주기 원본 문자열 (예: {@code 청년,중장년,노년}). Rule 3 입력. */
    @Column(length = 120)
    private String lifeArray;

    /** 대상특성 원본 문자열. 값이 없으면 빈 문자열. Rule 4 입력. */
    @Column(length = 255)
    private String trgterIndvdlArray;

    @Column(length = 255)
    private String intrsThemaArray;

    @Column(length = 120)
    private String srvPvsnNm;

    @Column(length = 40)
    private String sprtCycNm;

    @Column(length = 1)
    private String onapPsbltYn;

    @Column(length = 500)
    private String detailLink;

    /** 최초등록일 {@code YYYYMMDD} (목록 응답 {@code svcfrstRegTs}). 신선도 판정은 아직 안 한다. */
    @Column(length = 8)
    private String svcfrstRegTs;

    /** 기준연도 (상세 응답 {@code crtrYr}). Step 2(상세보강)가 붙기 전까지는 항상 {@code null}. */
    @Column(length = 4)
    private String crtrYr;

    /** 가중 스코어 합계. {@link YouthStatus#STRONG_YOUTH}면 {@code null}(스코어 계산을 건너뛴다). */
    private Integer ruleScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private YouthStatus youthStatus;

    /** 왜 그 스코어인지 사람이 읽는 흔적 (예: {@code R2+2,R3+3=5}). */
    @Column(length = 255)
    private String ruleTrace;

    /** 목록 응답의 해당 항목을 재직렬화한 XML. 나중 LLM 파싱 입력 + 변경 감지용. */
    @Column(columnDefinition = "TEXT")
    private String rawListXml;

    /** 상세 응답 원본 XML. Step 2가 붙기 전까지 {@code null}. */
    @Column(columnDefinition = "TEXT")
    private String rawDetailXml;

    /** 마지막으로 수집에 포함된 시각 (이번 실행 기준 시각). */
    @Column(nullable = false)
    private LocalDateTime collectedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── 지원금 매칭 API 보강 필드 (docs/welfare-api/matching-spec.md) ──
    // 목록·상세(SUBSIDY-009 / SUBSIDY-005)가 쓴다. 수집 배치가 채우는 건 후속 —
    // 지금은 data.sql 시드로만 채운다. 그래서 전부 nullable.

    /** 정규화 카테고리 (예: {@code 주거}). 프론트 칩 필터의 대상. */
    @Column(length = 40)
    private String category;

    /** 지원 대상 서술 (예: {@code 만 19세 ~ 34세 무주택 청년}). 검색 대상. */
    @Column(length = 1000)
    private String targetDescription;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SupportType supportType;

    /** 지원 금액. 돈이므로 {@link BigDecimal}. */
    @Column(precision = 15, scale = 2)
    private BigDecimal supportAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SupportAmountType supportAmountType;

    /**
     * 지원 기간(개월). {@code supportAmountType == MONTHLY}일 때 의미가 있다
     * (예: {@code 12} = "최대 12개월간 지원"). {@code null}이면 기간 미정/상시.
     * "예상 수입 변화" 카드(SUBSIDY-005)의 헤딩 개월 수로 쓰인다.
     */
    private Integer supportDurationMonths;

    /** 신청 마감일. {@code null}이면 마감 개념이 없는 제도. */
    private LocalDate applicationDeadline;

    /**
     * 상시 접수 여부. {@code true}면 마감일과 무관하게 항상 신청 가능.
     * {@code @ColumnDefault} — 기존 데이터가 있는 테이블에 NOT NULL 컬럼을 추가해도
     * {@code ddl-auto: update}의 ALTER가 실패하지 않도록 DB 기본값을 준다.
     */
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean ongoingApplication;

    /** 필요 서류. {@code |} 로 구분된 문자열 (서류명에 쉼표가 들어갈 수 있어 쉼표를 안 쓴다). */
    @Column(length = 1000)
    private String requiredDocuments;

    /** 신청처 이름 (예: {@code 복지로}). */
    @Column(length = 120)
    private String applyChannelName;

    @Column(length = 500)
    private String applyChannelUrl;

    @Column(length = 40)
    private String applyChannelPhone;

    // ── 큐레이션 품질 검증 결과 (docs/welfare-collector.md Step 4) ──
    // 수집 잡의 마지막 스텝이 채운다. null이면 아직 검증 전.

    /** {@code OK} / {@code NEEDS_REVIEW}. 리뷰 큐 조회 키. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CurationStatus curationStatus;

    /** 걸린 이슈들, 쉼표로 구분한 {@link WelfareIssue} 이름. {@code OK}면 {@code null}. */
    @Column(length = 500)
    private String curationIssues;

    private WelfareProgram(String servId, AgencyType agencyType) {
        this.servId = servId;
        this.agencyType = agencyType;
        this.source = ProgramSource.API_CANDIDATE;
    }

    /** 새로 잡힌 후보. */
    public static WelfareProgram fromCollection(WelfareListItem item, Classification classification, LocalDateTime collectedAt) {
        WelfareProgram program = new WelfareProgram(item.getServId(), item.getAgencyType());
        program.applyCollection(item, classification, collectedAt);
        return program;
    }

    /**
     * 이번 수집 결과로 필드를 덮어쓴다. 신규 행 생성과 기존 행 갱신이 같은 경로를 타도록
     * 한 메서드로 모아둔다.
     *
     * <p>{@link ProgramSource#MANUAL_CURATION} 행은 관리자가 소유하므로 {@code collectedAt}·
     * {@code rawListXml}(신선도·원문)만 갱신하고 나머지는 건드리지 않는다.
     */
    public void applyCollection(WelfareListItem item, Classification classification, LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
        this.rawListXml = item.getRawXml();
        if (this.source == ProgramSource.MANUAL_CURATION) {
            return;
        }

        this.servNm = item.getServNm();
        this.servDgst = clip(item.getServDgst(), 1000);
        this.jurMnofNm = item.getJurMnofNm();
        this.jurOrgNm = item.getJurOrgNm();
        this.lifeArray = item.getLifeArray();
        this.trgterIndvdlArray = item.getTrgterIndvdlArray() == null ? "" : item.getTrgterIndvdlArray();
        this.intrsThemaArray = item.getIntrsThemaArray();
        this.srvPvsnNm = clip(item.getSrvPvsnNm(), 120);
        this.sprtCycNm = clip(item.getSprtCycNm(), 40);
        this.onapPsbltYn = item.getOnapPsbltYn();
        this.detailLink = clip(item.getServDtlLink(), 500);
        this.svcfrstRegTs = item.getSvcfrstRegTs();
        this.agencyType = item.getAgencyType();
        this.ctpvNm = item.getCtpvNm();
        this.sggNm = item.getSggNm();
        this.bizChrDeptNm = clip(item.getBizChrDeptNm(), 255);
        this.aplyMtdNm = clip(item.getAplyMtdNm(), 100);
        this.lastModYmd = item.getLastModYmd();
        this.ruleScore = classification.score();
        this.youthStatus = classification.status();
        this.ruleTrace = classification.trace();

        deriveCuratedFields();
    }

    /**
     * 방금 채운 수집 필드에서 매칭 API용 필드를 파생한다.
     *
     * <p>목록 API로 확정할 수 있는 것만: {@code category}(관심주제 정규화),
     * {@code supportAmountType}(지원주기 매핑). 금액 <b>숫자</b>·필요서류·신청채널은
     * 상세 API의 자연어 필드라 상세보강 단계(후속)가 채운다 — 여기서는 건드리지 않는다.
     */
    private void deriveCuratedFields() {
        this.category = WelfareCategoryClassifier.classify(this.servNm, this.intrsThemaArray);
        this.supportAmountType = SupportCycleMapper.toAmountType(this.sprtCycNm);
        this.supportType = SupportTypeMapper.from(this.srvPvsnNm);
    }

    /**
     * 상세보강 Step이 상세 API에서 파싱한 값으로 채운다. 저장된 모든 제도가 이 경로를 탄다.
     * ({@code supportAmount}만 Processor에서 CASH로 걸러 넘어온다 — 비현금이면 {@code null}.)
     *
     * <p>{@code supportDurationMonths}는 목록 단계에서 채워졌을 수 있어(수집 시 파생) null이면 유지.
     * 나머지는 상세가 정본이라 그대로 덮어쓴다.
     *
     * <p>{@link ProgramSource#MANUAL_CURATION} 행은 {@code rawDetailXml}만 갱신하고 파싱값은
     * 건드리지 않는다 — 관리자가 소유.
     */
    public void applyDetail(BigDecimal supportAmount, Integer supportDurationMonths, String crtrYr,
                            String targetDescription,
                            String applyChannelName, String applyChannelUrl, String applyChannelPhone,
                            String rawDetailXml) {
        this.rawDetailXml = rawDetailXml;
        if (this.source == ProgramSource.MANUAL_CURATION) {
            return;
        }
        this.supportAmount = supportAmount;
        if (supportDurationMonths != null) {
            this.supportDurationMonths = supportDurationMonths;
        }
        this.crtrYr = crtrYr;
        this.targetDescription = clip(targetDescription, 1000);
        this.applyChannelName = clip(applyChannelName, 120);
        this.applyChannelUrl = clip(applyChannelUrl, 500);
        this.applyChannelPhone = clip(applyChannelPhone, 40);
    }

    /**
     * 검색·상세 API에 내보내도 되는 상태인가.
     * <ul>
     *   <li>{@link ProgramSource#MANUAL_CURATION} — 관리자가 검토·소유한 행이라 항상 노출</li>
     *   <li>그 외 — 검증에서 걸린 행({@code NEEDS_REVIEW})은 감춘다. 검증 전({@code null})은 노출
     *       (플래그는 "문제를 찾았다"는 denylist지 allowlist가 아니다)</li>
     * </ul>
     *
     * <p>이 규칙은 {@code WelfareProgramRepository#search}의 JPQL {@code where} 절과 짝이다.
     * 한쪽을 바꾸면 다른 쪽도 바꾼다.
     */
    public boolean isPubliclyVisible() {
        return this.source == ProgramSource.MANUAL_CURATION
                || this.curationStatus != CurationStatus.NEEDS_REVIEW;
    }

    /**
     * 품질 검증 스텝이 결과를 기록한다. 매 수집마다 다시 판정하므로 무조건 덮어쓴다.
     * 이슈가 없으면 {@code OK} + {@code curationIssues = null}.
     */
    public void applyCurationReview(List<WelfareIssue> issues) {
        if (issues.isEmpty()) {
            this.curationStatus = CurationStatus.OK;
            this.curationIssues = null;
            return;
        }
        this.curationStatus = CurationStatus.NEEDS_REVIEW;
        this.curationIssues = issues.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private static String clip(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
