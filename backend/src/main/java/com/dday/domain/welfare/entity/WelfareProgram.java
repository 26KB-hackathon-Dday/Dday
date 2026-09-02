package com.dday.domain.welfare.entity;

import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.collector.Classification;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 외부 API 서비스 ID (예: {@code WLF00004661}). upsert 키. */
    @Column(name = "serv_id", nullable = false, length = 20)
    private String servId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AgencyType agencyType;

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

    private WelfareProgram(String servId) {
        this.servId = servId;
        this.agencyType = AgencyType.CENTRAL;
        this.source = ProgramSource.API_CANDIDATE;
    }

    /** 새로 잡힌 후보. */
    public static WelfareProgram fromCollection(WelfareListItem item, Classification classification, LocalDateTime collectedAt) {
        WelfareProgram program = new WelfareProgram(item.getServId());
        program.applyCollection(item, classification, collectedAt);
        return program;
    }

    /**
     * 이번 수집 결과로 필드를 덮어쓴다. 신규 행 생성과 기존 행 갱신이 같은 경로를 타도록
     * 한 메서드로 모아둔다.
     */
    public void applyCollection(WelfareListItem item, Classification classification, LocalDateTime collectedAt) {
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
        this.ruleScore = classification.score();
        this.youthStatus = classification.status();
        this.ruleTrace = classification.trace();
        this.rawListXml = item.getRawXml();
        this.collectedAt = collectedAt;
    }

    private static String clip(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
