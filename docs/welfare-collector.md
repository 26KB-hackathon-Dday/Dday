# 복지서비스 수집기 (welfare-collector)

중앙부처 복지서비스 API를 매월 긁어와, **청년 대상 여부를 룰로 판정**해서 후보를
`welfare_program` 테이블에 쌓는 Spring Batch 잡.

지금까지 사람이 채팅으로 하던 일 — "이 servId XML 받아서 청년 맞는지 봐줘" — 을 코드로 옮긴 것이다.
API 스펙·필드 의미·룰의 근거는 [welfare-api/NOTES.md](./welfare-api/NOTES.md)에 있다. 여기서는 **동작**만 적는다.

---

## 1. 잡 흐름

```
매월 1일 00:00  (cron 0 0 0 1 * *)   —  WelfareCollectScheduler
        │
        ▼
┌─ Step 1  collectYouthListStep  (이번 PR은 이 스텝 하나뿐) ──────┐
│  목록 API 페이징 (numOfRows=10)                                │
│    srchKeyCode=003 & searchWrd=청년 & lifeArray=004            │
│  각 servList 항목마다:                                          │
│    ├─ 사전필터 (§NOTES 7-0)                                     │
│    │    통과 못 하면 → DISCARD (저장 안 함)                     │
│    └─ YouthClassifier 판정                                      │
│         ├─ Rule 1 매칭        → STRONG_YOUTH                    │
│         ├─ score ≥ 3          → AUTO_APPROVED                   │
│         ├─ 0 ≤ score < 3      → REVIEW_QUEUE                    │
│         └─ score < 0          → 저장 안 함                       │
│  저장 대상만 upsert (키: serv_id)                               │
└───────────────────────────────────────────────────────────────┘
        │
        ▼
   관리자는 youth_status = REVIEW_QUEUE 인 행만 확인하면 된다
```

**이번 PR에서 빠진 Step 2 (후속):** REVIEW_QUEUE 건을 상세 API로 보강 —
`crtrYr`·`raw_detail_xml` 저장, 상세 텍스트(`tgtrDtlCn` 등)에 Rule 1 키워드가 있으면
`STRONG_YOUTH`로 승격. "취업 후 상환 학자금대출" 같은 함정 케이스를 여기서 건진다.
그전까지 `crtr_yr`·`raw_detail_xml` 컬럼은 항상 `null`.

- **신선도 / 종료 판정은 이번 범위 아님.** `svcfrstRegTs`는 저장만.
- **LLM 파싱은 이번 범위 아님.**

---

## 2. 판정 룰 (요약 — 근거는 NOTES §7)

### 사전필터

`자립준비청년`·`보호종료` 키워드가 있거나, `"청년"`이 `servNm`·`servDgst`·`jurOrgNm` 중 하나에
있으면 통과. 아니면 버린다.

### Rule 1 — 절대

`servNm`/`servDgst`에 `자립준비청년` 또는 `보호종료` → **STRONG_YOUTH 즉시 확정**.

### Rule 2~5 — 가중 스코어

| 룰 | 조건 | 점수 |
|---|---|---|
| Rule 2 | `jurOrgNm`에 `청년` | +2 |
| Rule 3 | `lifeArray` == `[청년]` 단독 | +3 |
| Rule 3 | `lifeArray` 생애주기 3개 이상 | -2 |
| Rule 4 | `trgterIndvdlArray` 2개 이상 | -3 |
| Rule 5 | `servDgst` 가 `청년`으로 시작 | +1 |
| Rule 5 | `servDgst` 의 `청년` 이 나열의 일부 | -1 |

**임계값: ≥3 자동승인 · 0~2 검토큐 · <0 제외(미저장).**

### Rule 5 나열 판정 (결정론적 근사)

`servDgst` 안 `청년`의 앞뒤 15자에 `,` / `·` / `및` 와 함께 다른 인물명사
(`신혼부부` `대학생` `한부모` `가족` `어르신` `아동` `여성` `장애인` `노인` `중장년`)가 나오면
"나열의 일부"로 보고 -1. 명사 목록은 `Rule5DgstPosition` 상수.

---

## 3. 저장

`welfare_program` 테이블. 스키마 컬럼은 NOTES §8.

- **upsert 키 = `serv_id`.** 재실행 시 갱신 (멱등).
- **저장하는 status**: `STRONG_YOUTH` / `AUTO_APPROVED` / `REVIEW_QUEUE`.
- 저장 안 함: 사전필터 DISCARD, score < 0 (`AUTO_REJECTED` 개념) — enum에도 없다.
- `raw_list_xml` 원본 보존 (항목을 JAXB로 재직렬화한 `<servList>`) — 나중 LLM 파싱 입력 + diff.
  `raw_detail_xml` / `crtr_yr` 는 Step 2(후속)가 채운다. 이번엔 항상 `null`.
- `rule_trace` 예: `"R2+2,R3+3,R5-1=4"` — 왜 그 점수인지 사람이 읽는 용도.
- 이전 실행엔 있었는데 이번엔 사라진 `serv_id`: 지우지 않는다. `collected_at`이 갱신 안 되므로
  `where collected_at < :thisRun` 으로 "이번에 안 잡힌 것"을 조회할 수 있다.

---

## 4. 외부 호출

| | |
|---|---|
| base URL | `http://apis.data.go.kr/B554287/NationalWelfareInformationsV001` (`welfare.api.base-url`) |
| 목록 | `GET /NationalWelfarelistV001` |
| 상세 | `GET /NationalWelfaredetailedV001` |
| 인증 | 쿼리 `serviceKey` = `welfare.api.service-key` (환경변수 `WELFARE_API_KEY`, Decoding 키) |
| 클라이언트 | `RestClient`로 문자열 수신 → `WelfareXml`(JAXB)로 파싱 |
| 성공 판정 | 응답 `<resultCode>0</resultCode>` (표준 `00` 아님). 그 외엔 `WelfareApiException` → Step 재시도/실패 |
| 타임아웃 | connect 3s / read 10s |
| 재시도 | Step 청크 단위 Spring Batch 재시도 3회 (`WelfareApiException`) |

> **왜 JAXB인가:** `jackson-dataformat-xml`을 넣으면 `MappingJackson2XmlHttpMessageConverter`가
> 등록돼 우리 JSON API 응답까지 XML로 바뀐다(HealthControllerTest가 깨진다). JAXB의
> `Jaxb2RootElementHttpMessageConverter`는 `@XmlRootElement` 타입만 다뤄 부작용이 없다.

- **콜 예산**: 목록 ~3콜 + 상세 = 검토큐 건수(월 한 자릿수). 개발키 1,000/일과 무관.

---

## 5. 배치 실행 상세

- 엔진: Spring Batch (`spring-boot-starter-batch`). `BATCH_*` 메타테이블은
  `spring.batch.jdbc.initialize-schema: always` 로 생성.
- **`spring.batch.job.enabled: false`** — 앱 기동 시 자동 실행 막는다. (CI·배포마다 외부 API를
  때리면 안 된다. CI엔 키도 없다.)
- 실행 트리거: `WelfareCollectScheduler` 의 `@Scheduled(cron = "${welfare.collect.cron}")` 이
  `JobLauncher.run()` 호출. cron 기본값 `0 0 0 1 * *`.
- `@Scheduled` 활성화를 위해 `@EnableScheduling` (welfare 도메인 config 또는 global).
- `JobParameters` 에 `runAt=<epochMillis>` 를 넣어 매 실행을 새 인스턴스로 (Batch는 파라미터가
  같으면 재실행을 거부한다).

### 로컬에서 수동 실행

```bash
# application-local.yml 에 실제 키를 넣거나 환경변수로
WELFARE_API_KEY=xxxx ./gradlew bootRun
```

그다음 cron을 안 기다리고 바로 돌리려면 (`local` 프로파일 전용):

```bash
curl -X POST http://localhost:8080/internal/welfare/collect
```

`WelfareCollectDevController` — `@Profile("local")` 이라 운영엔 안 뜬다. 잡은 동기로 돈다.

---

## 6. 패키지 구조

```
com.dday.domain.welfare
├─ entity/
│  ├─ WelfareProgram.java        @Entity
│  ├─ YouthStatus.java           STRONG_YOUTH / AUTO_APPROVED / REVIEW_QUEUE
│  ├─ AgencyType.java            CENTRAL / LOCAL
│  └─ ProgramSource.java         API_CANDIDATE
├─ repository/
│  └─ WelfareProgramRepository.java
├─ client/
│  ├─ NationalWelfareApiClient.java   목록 페이지 조회
│  ├─ WelfareClientConfig.java        RestClient 빈 (타임아웃)
│  ├─ WelfareXml.java                 JAXB 파싱/재직렬화 (정적)
│  ├─ WelfareApiException.java
│  └─ dto/  (WelfareListResponse, WelfareListItem)
├─ collector/
│  ├─ YouthClassifier.java       사전필터 + 룰 조합 + 판정
│  ├─ Classification.java        결과(disposition, status, score, trace)
│  └─ rule/
│     ├─ YouthPreFilter.java  Rule1StrongKeyword  Rule2JurOrgName
│     ├─ Rule3LifeStage  Rule4TargetCount  Rule5DgstPosition
│     └─ RuleHit.java
├─ batch/
│  ├─ WelfareCollectJobConfig.java   Job + Step 1
│  ├─ WelfareListReader.java         @StepScope, API 페이징 ItemReader
│  ├─ WelfareClassifyProcessor.java  ItemProcessor (null = 미저장)
│  ├─ WelfareProgramWriter.java      @StepScope, upsert
│  ├─ ClassifiedProgram.java         Processor→Writer 묶음
│  ├─ WelfareCollectLauncher.java    JobLauncher 호출 (스케줄러·컨트롤러 공용)
│  ├─ WelfareCollectScheduler.java   @Scheduled(cron)
│  └─ WelfareBatchConfig.java        @EnableScheduling
├─ api/
│  └─ WelfareCollectDevController.java  @Profile("local") 수동 트리거
└─ dto/
   └─ WelfareSuccessCode.java
```

`domain/pocket` 컨벤션(엔티티 `@Setter` 금지·`@Builder`·이름 있는 변경 메서드)을 그대로 따른다.
서비스 클래스는 아직 없다 — 수집은 batch가, 판정은 `collector`가 한다. 관리자 검토 조회 API는 후속.

---

## 7. 테스트

- `YouthClassifierTest` — NOTES §7-4 의 10건을 **실제 응답 XML**(`src/test/resources/welfare/list-central.xml`)
  로 fixture 삼아 각 건의 disposition·status·score 검증. **이 PR의 핵심 회귀 테스트** — 룰 가중치를
  바꾸면 여기부터 깨진다.
- `Rule3LifeStageTest`, `Rule5DgstPositionTest` — 경계값 (단독/2개/3개, 시작/나열/무관).
- `WelfareListResponseParseTest` — `<wantedList>` 고유 구조·대상특성 없음·값 안의 `·` 파싱.
- `DdayApplicationTests`(`@SpringBootTest`)가 Batch 빈·스케줄러·JAXB까지 뜨는지 — CI MySQL 필요.

---

## 8. 이번 PR에서 빠지는 것 (후속)

- **Step 2 상세보강** — REVIEW_QUEUE 건 상세 API 호출, `crtrYr`·`raw_detail_xml` 저장,
  상세 텍스트로 Rule 1 재검사 → STRONG_YOUTH 승격.
- 지자체(LOCAL) API — 응답 스키마·`lastModYmd` 기반 신선도가 달라서 분리.
- 신선도 / 종료 감지 (CENTRAL diff, `crtrYr` 활용).
- LLM 파싱 (`raw_list_xml`/`raw_detail_xml` → 구조화 필드).
- 관리자 검토 UI / API (`REVIEW_QUEUE` 조회·승인·반려).
