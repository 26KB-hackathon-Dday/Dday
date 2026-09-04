# 외부 API 스펙 — 복지서비스 수집기 (CENTRAL)

> 수집 배치가 호출할 외부 API 정리. 태그: **【확인】** 실제 응답 XML로 확정 / **【추정】** / **【❓】** 미해결.
>
> **범위: 중앙부처복지서비스(CENTRAL) — §1~10.** 지자체(LOCAL)는 §11 (진행 중).
> 샘플: [`list-central.xml`](./list-central.xml) (목록, 28건 중 1페이지), [`detail-central.xml`](./detail-central.xml) (상세 1건),
> [`list-local.xml`](./list-local.xml) · [`detail-local.xml`](./detail-local.xml) (LOCAL).

---

## 1. 대상 API

| | |
|---|---|
| 이름 | 한국사회보장정보원_중앙부처복지서비스 |
| publicDataPk | `15090532` |
| 문서 | <https://www.data.go.kr/data/15090532/openapi.do> |
| 응답 포맷 | XML 【확인】 |
| 트래픽 | 개발계정 1,000회/일 【확인】 |
| serviceKey | 하나의 키로 CENTRAL·LOCAL 모두 호출 가능 【확인】 |
| `agency_type` | `CENTRAL` |

---

## 2. 엔드포인트

| 오퍼레이션 | 경로 |
|---|---|
| 목록조회 | `.../NationalWelfarelistV001` 【확인 — 접미사】 |
| 상세조회 | `.../NationalWelfaredetailedV001` 【확인 — 접미사】 |

> host + 상위 경로(`apis.data.go.kr/B554287/NationalWelfareInformationsV001`)는 【추정】.
> **【❓ 샘플을 뽑은 실제 호출 URL 전체 (파라미터 포함) 알려주세요.】** — 특히 목록 URL에
> `searchWrd`가 실제로 들어갔는지 확인 필요 (§4-3 참고).

---

## 3. 인증 / 설정

- 쿼리파라미터 `serviceKey` 【확인】
- 환경변수(제안): `WELFARE_API_KEY` — `application-*.yml`에서 참조, 커밋 파일엔 값 없음.

---

## 4. 목록조회

### 4-1. 요청 파라미터

| 파라미터 | 값 | 확인 |
|---|---|---|
| `serviceKey` | `{{SERVICE_KEY}}` | 【확인】 |
| `pageNo` | 1,2,3… | 【확인】 |
| `numOfRows` | `10` | 【확인】 |
| `srchKeyCode` | `003` | 【확인 — 사용자】 생애주기 검색 모드로 추정 |
| `lifeArray` | `004` (청년) | 【추정 — 코드 `004`, 첫 실행에서 검증】 |
| `searchWrd` | `청년` | `srchKeyCode=003`과 함께 보냄 |

> **【❓ 낮은 우선순위】** 샘플 뽑을 때 `srchKeyCode`가 없어서 `searchWrd`가 무시됐던 것으로 보임
> (§4-3). `srchKeyCode=003`을 넣으면 결과가 좁혀지는지는 **첫 실전 실행에서 `totalCount` 변화로 확인.**
> 좁혀지지 않아도 우리 Rule이 걸러내므로 설계엔 영향 없음.

### 4-2. 페이징

```
pageNo = 1
loop:
  GET 목록(srchKeyCode=003, searchWrd=청년, lifeArray=004, pageNo, numOfRows=10)
  <servList> 각각 처리
  if pageNo * 10 >= <totalCount>: break
  pageNo++
```

- 샘플: `<totalCount>28</totalCount>` (srchKeyCode 없이) → 3페이지.
- 성공 판정: `<resultCode>0</resultCode>` (표준 data.go.kr의 `00`이 아니라 **`0`**), `<resultMessage>SUCCESS`.

### 4-3. 참고 — 샘플은 lifeArray만으로 뽑힘

샘플 10건에 `예술활동준비금 지원`(WLF00003199) 포함 — servNm·servDgst·jurOrgNm 어디에도 "청년"
없고 `lifeArray`에만 `청년`. 즉 샘플은 `lifeArray=004`만 적용됐고 `searchWrd`는 무시됨.
실전에선 `srchKeyCode=003`을 더해 좁혀본다. 안 좁혀져도 Rule이 처리.

### 4-4. 목록 응답 스키마 【확인】

루트 `<wantedList>`, 그 안에:

| 엘리먼트 | 의미 | 비고 |
|---|---|---|
| `totalCount` `pageNo` `numOfRows` | 페이징 | |
| `resultCode` `resultMessage` | `0` / `SUCCESS` | |
| `servList` (반복) | 서비스 1건 | 아래 |

`<servList>` 필드:

| 필드 | 예시 | 판정 | 비고 |
|---|---|:---:|---|
| `servId` | `WLF00004661` | PK | |
| `servNm` | `청년월세 지원사업` | Rule 0·1 | |
| `servDgst` | 요약 (개행 포함 가능) | Rule 0·1·5 | |
| `lifeArray` | `청년` / `청년,중장년,노년` | **Rule 3** | **한글명, `,` 구분, 공백 없음** |
| `trgterIndvdlArray` | `저소득` / `저소득,한부모·조손` | **Rule 4** | **한글명, `,` 구분. `한부모·조손`처럼 값 안에 `·` 있음 → `,`로만 split.** **값 없으면 엘리먼트 자체가 없음** |
| `intrsThemaArray` | `주거` / `생활지원,서민금융` | — | 저장만 |
| `jurMnofNm` | `국토교통부` | — | 소관 부처 |
| `jurOrgNm` | `청년주거정책과` | **Rule 2** | 소관 부서 |
| `svcfrstRegTs` | `20220413` | (약한 신선도?) | **최초등록일 YYYYMMDD — 목록에 존재.** 수정일은 아님 |
| `servDtlLink` | 복지로 URL (`&amp;` 포함) | 저장 | |
| `sprtCycNm` | `월` `1회성` `수시` `년` `반기` | 저장 | |
| `srvPvsnNm` | `현금지급` `현물지급` `현금대여(융자)` … | 저장 | |
| `onapPsbltYn` | `Y`/`N` | 저장 | 온라인신청 가능 |
| `inqNum` | `12572988` | 저장 | 조회수 |
| `rprsCtadr` | `1599-0001` | 저장 | 대표 연락처 |
| 수정일 필드 | **없음** | — | 목록으로 종료 여부 판정 불가 |

---

## 5. 상세조회 (애매한 건만)

- **호출 대상: `youth_status = REVIEW_QUEUE` 인 건만.** 자동승인/자동제외/DISCARD는 상세 안 부름.
- 요청: `servId` (필수) 【추정 — URL 확인 필요】

### 5-1. 상세 응답 스키마 【확인】

루트 `<wantedDtl>` (단건):

| 필드 | 예시 | 용도 |
|---|---|---|
| `servId` `servNm` | `WLF00001175` / `자립준비청년 자립수당 지급` | 식별 |
| `crtrYr` | `2026` | **기준연도 — 유일한 신선도 단서. 저장만 (이번엔 판정 안 함)** |
| `wlfareInfoOutlCn` | 서비스 개요/목적 | Rule 1 재검사, LLM 입력 |
| `tgtrDtlCn` | 지원대상 상세 (`자립준비청년`, `보호종료`, `가정위탁` 다수) | **Rule 1 재검사**, LLM 입력 |
| `slctCritCn` | 선정기준 | LLM 입력 |
| `alwServCn` | 지원내용 (`매월 50만원`) | LLM 입력 |
| `lifeArray` | `청년, 청소년` | **주의: 상세는 `, ` (콤마+공백)** |
| `intrsThemaArray` | `생활지원` | |
| `sprtCycNm` `srvPvsnNm` `rprsCtadr` | | |
| `applmetList` (반복) | `servSeCode`/`servSeDetailLink`/`servSeDetailNm` — 신청 절차 단계 | LLM 입력 |
| `inqplCtadrList` (반복) | 문의처 | |
| `inqplHmpgReldList` (반복) | 관련 사이트 | |
| `baslawList` (반복) | 근거 법령 (`아동복지법`) | |
| `resultCode` `resultMessage` | `0` / `SUCCESS` | |
| `jurMnofNm` | `보건복지부 청년정책팀` | **주의: 상세는 부처+부서가 한 필드에 합쳐짐** (목록은 분리) |

> 상세엔 `jurOrgNm` 분리 필드가 없음 → Rule 2 재검사는 상세 `jurMnofNm` 문자열에 "청년" 포함으로.

---

## 6. 신선도(종료 여부) — 이번엔 보류

- 목록엔 수정일 없음, 상세엔 `crtrYr`(연도)뿐. 목록 단계 신선도 판정 불가.
- **이번 PR: 신선도/종료 판정 구현 안 함.** `crtrYr`, `svcfrstRegTs`는 저장만 해두고 나중에 활용.

---

## 7. 청년 대상 판별 룰

### 7-0. 사전 필터 (스코어링 전) — **확정**

```
if (Rule1 키워드 매칭)              → 사전필터 통과 (그리고 STRONG_YOUTH)
else if ("청년" in servNm || servDgst || jurOrgNm)  → 사전필터 통과 → 스코어링
else                               → DISCARD (저장 안 함)
```

- 근거: 학자금대출은 servNm·servDgst엔 "청년" 없지만 `jurOrgNm=청년장학지원과` → 통과 → 큐 →
  상세조회에서 자립준비청년 특례 발견. 통합공공임대·기존주택·예술활동준비금·주거안정월세대출은 DISCARD.

### 7-1. Rule 1 — 자립준비청년 전용 (최우선, 절대)

- 키워드: **`자립준비청년`, `보호종료`** (`가정위탁`은 제외 — 사용자 결정)
- `servNm` / `servDgst` (상세 단계면 `tgtrDtlCn`·`slctCritCn`·`wlfareInfoOutlCn`도) 에 하나라도 포함
  → **`STRONG_YOUTH` 즉시 확정, 스코어 계산 생략.**

### 7-2. Rule 2~5 — 가중 스코어 **(확정)**

| 룰 | 조건 | 점수 |
|---|---|---|
| Rule 2 | `jurOrgNm`(상세면 `jurMnofNm`)에 `"청년"` 포함 | +2 |
| Rule 3 | `lifeArray` split 결과가 `[청년]` 단독 | **+3** |
| Rule 3 | `lifeArray` 생애주기 **3개 이상** | -2 |
| Rule 4 | `trgterIndvdlArray` 값 0개 또는 1개 | 0 |
| Rule 4 | `trgterIndvdlArray` 값 **2개 이상** | -3 |
| Rule 5 | `servDgst.trim()`이 `"청년"`으로 시작 | +1 |
| Rule 5 | `servDgst`에서 `"청년"` 앞/뒤 15자에 `,`·`·`·`및` + 다른 인물명사(신혼부부/대학생/한부모/가족/어르신/아동/여성/장애인) | -1 |
| **Rule 6** | **`servNm`에 `"청년"` 포함** | **+2** |

> - Rule 3 단독을 +2 → **+3** 으로 올림 (§7-5 캘리브레이션).
> - Rule 5 나열 판정은 결정론적 근사 유지.
> - Rule 6은 28건 전수 검증에서 추가 — "청년창업농장학금 지원"은 요약에 "청년"이 없고(→ "농업 후계인력")
>   `lifeArray`가 3개(청년·중장년·노년)라 Rule 3에서 -2를 맞아 자동 제외됐다. 서비스명에 "청년"이 박힌 건
>   그 사업이 청년을 겨냥했다는 뜻이라 +2. ("가족돌봄청년"류는 추가 안 함 — 자립준비청년은 가족이 없어
>   애초에 대상 모집단이 다르다.)

### 7-3. 최종 판정 **(확정: T_hi=3, T_lo=0)**

| | status | 저장 |
|---|---|---|
| Rule 1 매칭 | `STRONG_YOUTH` | O |
| 스코어 ≥ 3 | `AUTO_APPROVED` | O |
| 0 ≤ 스코어 < 3 | `REVIEW_QUEUE` (+ 상세조회) | O |
| 스코어 < 0 | `AUTO_REJECTED` | **X (저장 안 함 — 사용자 결정)** |

### 7-4. 28건 전수 검증 (Rule 6 포함, T_hi=3, T_lo=0)

`lifeArray=004 & searchWrd=청년` → 28건. 회귀 테스트: `YouthClassifierFullSetTest`.

| 결과 | 건수 |
|---|---|
| STRONG_YOUTH | 3 |
| AUTO_APPROVED | 8 |
| REVIEW_QUEUE | 4 |
| 미저장 — 스코어 < 0 | 4 |
| 미저장 — 사전필터 탈락 | 9 |
| **저장 합계** | **15** |

주요 케이스:

| servId | servNm | 흔적 | 결과 |
|---|---|---|---|
| WLF00001175 / 6307 / 5445 | 자립준비청년·자립지원 전담 | R1 | STRONG_YOUTH |
| WLF00004661 | 청년월세 | R2+2,R3+3,R6+2 = 7 | AUTO_APPROVED |
| WLF00000060 | 청년내일저축계좌 | R3+3,R6+2 = 5 | AUTO_APPROVED |
| WLF00006266 | 청년미래적금 | R2+2,R3+3,R5+1,R6+2 = 8 | AUTO_APPROVED |
| WLF00001076 | 햇살론youth | R3+3,R5-1 = 2 (servNm은 "youth", R6 없음) | REVIEW_QUEUE |
| WLF00003277 | 학자금대출 | R2+2 = 2 (jurOrg 청년장학지원과) | REVIEW_QUEUE |
| WLF00000812 | 청년창업농장학금 | R3-2,R6+2 = 0 | REVIEW_QUEUE (Rule 6이 구제) |
| WLF00004649 | 행복주택 | R3-2,R4-3,R5-1 = -6 | 미저장 |
| WLF00005411 | 일상돌봄 (가족돌봄청년) | R3-2 = -2 | 미저장 (자립준비청년은 가족 없음 → 대상 아님) |
| WLF00003245 | 국민취업지원제도 | R3-2,R5-1 = -3 | 미저장 |
| WLF00005567 등 9건 | 심리상담·농식품바우처·서민금융교육 등 | 사전필터 | 미저장 (전국민/전연령) |

> 오탐 없음 — 승인 8건 전부 진짜 청년 사업, DISCARD 9건 전부 전연령. Step 2(상세보강)가 붙으면
> REVIEW_QUEUE 4건 중 자립준비청년 특례가 있는 건이 STRONG_YOUTH로 올라간다.

### 7-5. 상세조회 후 재판정

- REVIEW_QUEUE 건만 상세조회. 상세 텍스트(`tgtrDtlCn` 등)에 Rule 1 키워드 있으면 → `STRONG_YOUTH`로 승격.
- 그 외엔 `REVIEW_QUEUE` 유지 (관리자가 최종 판단). 상세 스코어 재계산은 하지 않음 (텍스트가 길어 Rule 5 왜곡).
- `crtrYr` 저장.

---

## 8. WelfareProgram 저장 스키마 (신규 생성)

새 도메인 `com.dday.domain.welfare`. `pocket` 도메인 구조 복제.

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `id` | PK auto | |
| `serv_id` | varchar(20), **unique** | `WLF00004661` — upsert 키 |
| `agency_type` | enum(`CENTRAL`,`LOCAL`) | |
| `source` | enum(`API_CANDIDATE`,…) | |
| `serv_nm` | varchar(255) | |
| `serv_dgst` | varchar(1000) | |
| `jur_mnof_nm` | varchar(120) | 부처 |
| `jur_org_nm` | varchar(120) | 부서 (Rule 2) |
| `life_array` | varchar(120) | 원본 문자열 그대로 |
| `trgter_indvdl_array` | varchar(255) | 원본 문자열 (없으면 `""`) |
| `intrs_thema_array` | varchar(255) | |
| `srv_pvsn_nm` | varchar(120) | |
| `sprt_cyc_nm` | varchar(40) | |
| `onap_psblt_yn` | char(1) | |
| `detail_link` | varchar(500) | |
| `svcfrst_reg_ts` | varchar(8) | 목록에서 |
| `crtr_yr` | varchar(4) | 상세에서만 (nullable) |
| `rule_score` | int (nullable) | Rule 1이면 null |
| `youth_status` | enum(`STRONG_YOUTH`,`AUTO_APPROVED`,`REVIEW_QUEUE`) | `AUTO_REJECTED`는 저장 안 하므로 enum에 없음 |
| `rule_trace` | varchar(255) | `"R2+2,R3+3"` 디버그 |
| `raw_list_xml` | text | `<servList>` 원본 |
| `raw_detail_xml` | text (nullable) | `<wantedDtl>` 원본 |
| `collected_at` | datetime | 잡 파라미터 `runAt`(모든 스텝 공유) |
| `created_at` `updated_at` | `@CreationTimestamp`/`@UpdateTimestamp` | |
| `ctpv_nm` `sgg_nm` `biz_chr_dept_nm` `aply_mtd_nm` `last_mod_ymd` | — | **지자체(LOCAL) 전용 — §11-6.** CENTRAL은 전부 null |

- **upsert 키 = `serv_id`.** 재실행 시 갱신(멱등).
- **저장하는 것: `STRONG_YOUTH` / `AUTO_APPROVED` / `REVIEW_QUEUE` 만.**
- 저장 안 하는 것: 사전필터 DISCARD, `AUTO_REJECTED`(스코어 < 0) — 사용자 결정.
  - ⚠️ 재실행 시 이전엔 저장됐는데 이번에 제외/DISCARD로 바뀐 `serv_id`는 어떻게? → **soft-delete
    안 하고 그냥 남겨두되 `collected_at`이 안 갱신됨 → "이번 수집에 없던 건"으로 조회 가능.** (구현 시 확정)

---

## 9. 배치 실행 — **확정**

| | |
|---|---|
| 주기 | **매월 1일 00:00** (cron `0 0 0 1 * *`) |
| 엔진 | **Spring Batch 정식 도입** (`spring-boot-starter-batch`) |
| 메타테이블 | `BATCH_*` — MySQL에 생성 필요. `spring.batch.jdbc.initialize-schema: always` + `data.sql`과 실행순서 주의 (§구현) |
| Job | `welfareCollectJob` |
| Step 1 | `collectYouthListStep` — 목록 페이징 → 사전필터 → Rule → upsert (chunk) |
| Step 2 | `enrichReviewQueueStep` — `REVIEW_QUEUE` 건 상세조회 → `crtrYr` + Rule 1 재검사 |
| 콜 예산 | 목록 ~3콜 + 상세 = 큐 건수(한 자릿수 예상). 1,000/일 제한에 전혀 안 걸림 |

---

## 10. 남은 질문 (전부 낮은 우선순위 — 구현 병행 가능)

| # | 질문 | 기본값(이걸로 진행) |
|---|---|---|
| 1 | 목록 쿼리 파라미터 | **확정: `srchKeyCode=003 & searchWrd=청년 & lifeArray=004`** (Swagger에서 함께 전송 확인) |
| 3 | `lifeArray` 청년 코드 | **확정: `004`** |
| — | base URL | **확정: `http://apis.data.go.kr/B554287/NationalWelfareInformationsV001`** (`application.yml` 설정값) |

> 전부 확정. 사전필터(§7-0), Rule 1 키워드=`자립준비청년`·`보호종료`, Rule3 단독 +3, T_hi=3/T_lo=0,
> `AUTO_REJECTED` 미저장, 매월 1일 00:00, Spring Batch 정식.
> base URL `http://apis.data.go.kr/B554287/NationalWelfareInformationsV001`, 목록 `/NationalWelfarelistV001`, 상세 `/NationalWelfaredetailedV001`.

---

## 11. 지자체(LOCAL) 복지서비스 — 【확인 진행 중】

> 샘플: [`list-local.xml`](./list-local.xml) (목록, `totalCount=3`), [`detail-local.xml`](./detail-local.xml) (상세 1건 — `WLF00004197`).

### 11-1. 대상 API

| | |
|---|---|
| 이름 | 한국사회보장정보원_지자체복지서비스 |
| base URL | `http://apis.data.go.kr/B554287/LocalGovernmentWelfareInformations` |
| 목록 | `GET /LcgvWelfarelist` 【확인 — 사용자】 |
| 상세 | `GET /LcgvWelfaredetailed` 【확인 — 사용자】 |
| serviceKey | CENTRAL과 같은 키 (§1) |
| `agency_type` | `LOCAL` |
| 응답 포맷 | XML. 성공 판정 `<resultCode>0</resultCode>` — CENTRAL과 동일 |

### 11-2. 조회 전략 — **좁게** 【확인】

목록 요청 파라미터: `serviceKey` · `pageNo` · `numOfRows` · `lifeArray=004` · `searchWrd=자립준비청년`.
**`srchKeyCode`는 안 보낸다.** (CENTRAL과 달리 `searchWrd`가 그냥 먹는다 — `totalCount=3`이 그 증거,
안 먹었으면 청년 전체 수백 건.)

- `searchWrd=자립준비청년` → `totalCount=3`. 결과가 전부 자립준비청년 전용 지자체 제도다.
- CENTRAL은 `lifeArray=004`(청년 전체)로 넓게 긁고 룰로 걸렀지만, LOCAL은 **키워드로 좁혀서** 긁는다.
  D-1825가 필요한 건 자립준비청년 대상 제도뿐이고, LOCAL "범용 청년"(청년월세 지자체판 등)까지
  넓히면 수백 건 + 검토부담이라 이번 범위 아님.
- 지역 파라미터(`ctpvCd`/`sggCd`)를 안 보내면 전국이 한 번에 온다 (샘플이 서울 용산·서대문 + 전남광주 혼재).
  → **17개 시도 순회 불필요.**
- 놓칠 수 있는 것: "보호종료아동"으로만 표기된 제도. 필요하면 `searchWrd=보호종료`로 2차 수집 (후속).

### 11-3. 목록 필드 — CENTRAL 대조

| 의미 | CENTRAL (`NationalWelfarelistV001`) | LOCAL (`LcgvWelfarelist`) | 비고 |
|---|---|---|---|
| 서비스 ID | `servId` | `servId` | 둘 다 `WLF########`. bokjiro 전역 유니크 — CENTRAL/LOCAL 안 겹침 |
| 서비스명 / 요약 | `servNm` / `servDgst` | 동일 | |
| 상세링크 | `servDtlLink` (`…ReldBztpCd=01`) | `servDtlLink` (`…ReldBztpCd=02`) | `02` = 지자체 |
| 생애주기 | `lifeArray` (`청소년,청년` — 공백 X) | `lifeNmArray` (`청소년, 청년` — **공백 O**) | **엘리먼트명·구분자 다름** |
| 관심주제 | `intrsThemaArray` (공백 X) | `intrsThemaNmArray` (`보호·돌봄, 서민금융` — **공백 O**) | **엘리먼트명·구분자 다름** |
| 대상특성 | `trgterIndvdlArray` | **없음** | Rule 4 입력 소스 없음 → 항상 중립 |
| 소관 | `jurMnofNm` + `jurOrgNm` (분리) | `bizChrDeptNm` (`서울특별시 용산구 생활지원국 아동청소년과` — **한 필드**) | 부처/부서 분리 안 됨 |
| 시도 | 없음 | `ctpvNm` (`서울특별시`) | ★ 지역 |
| 시군구 | 없음 | `sggNm` (`용산구`) | ★ 지역. **시도 단위 사업이면 엘리먼트 없음** (자립정착금 `WLF00001803`) |
| 신청방법 | `onapPsbltYn` (Y/N) | `aplyMtdNm` (`방문, 전화, 우편`) | 성격 다름 |
| 최초등록일 | `svcfrstRegTs` | **없음** | |
| 최종수정일 | **없음** | `lastModYmd` (`20260723`) | ★ 신선도 단서 |
| 조회수 / 주기 / 제공유형 | `inqNum` / `sprtCycNm` / `srvPvsnNm` | 동일 | |
| 대표연락처 | `rprsCtadr` | 없음 | |

### 11-4. 상세 필드 — CENTRAL 대조

| 의미 | CENTRAL (`…detailedV001`) | LOCAL (`LcgvWelfaredetailed`) |
|---|---|---|
| 기준연도 | `crtrYr` | 없음 |
| 시행 시작/종료 | 없음 | `enfcBgngYmd` / `enfcEndYmd` (`99991231` = 무기한) ★ 종료 판정 |
| 지원대상 | `tgtrDtlCn` | `sprtTrgtCn` |
| 선정기준 / 지원내용 | `slctCritCn` / `alwServCn` | 동일 |
| 개요 | `wlfareInfoOutlCn` | 없음 |
| 신청방법 | `applmetList` (반복) | `aplyMtdCn` (텍스트) + `aplyMtdNm` |
| 문의처 | `inqplCtadrList` (`servSeCode`/`servSeDetailLink`/`servSeDetailNm`) | `inqplCtadrList` (`wlfareInfoDtlCd`/`wlfareInfoReldCn`/`wlfareInfoReldNm`) — **자식 필드명 다름** |
| 근거법령 | `baslawList` | `baslawList` (조례) + `basfrmList` (신청서식 파일) |
| 관련 홈페이지 | `inqplHmpgReldList` | 없음 |

### 11-5. 판정 룰 영향

1. 좁게 조회하므로 3건 전부 Rule 1(`자립준비청년`·`보호종료`) 매칭 → **STRONG_YOUTH**. 스코어링 룰(2~6)은
   LOCAL에서 사실상 무의미하지만, CENTRAL `YouthClassifier`를 그대로 재사용해도 안전한 쪽으로 수렴한다.
2. `trgterIndvdlArray` 없음 → Rule 4 항상 0.
3. `bizChrDeptNm`을 `jurOrgNm` 자리에 매핑하면 Rule 2 동작 (대개 "아동청소년과"라 0).
4. `lifeNmArray`·`intrsThemaNmArray`는 `, ` (공백 포함) → 파싱 시 정규화 (CENTRAL 상세와 동일 처리).
5. `sggNm` 없는 행 = 광역(시도) 단위 사업 → `sgg_nm = null`.

### 11-6. 저장 스키마 추가분 (§8에 더함)

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `ctpv_nm` | varchar(30) nullable | 시도명. CENTRAL은 null |
| `sgg_nm` | varchar(30) nullable | 시군구명. 광역 사업·CENTRAL은 null |
| `biz_chr_dept_nm` | varchar(255) nullable | LOCAL 사업담당부서 (CENTRAL은 `jur_mnof_nm`+`jur_org_nm` 사용) |
| `last_mod_ymd` | varchar(8) nullable | LOCAL 최종수정일. 종료 감지(후속)용 |
| `aply_mtd_nm` | varchar(100) nullable | 신청방법 요약 |

> `region_code`(행정표준코드) 정규화는 후속 — 지금은 `ctpv_nm`/`sgg_nm` 문자열만.
> `enfc_bgng_ymd`/`enfc_end_ymd`는 상세에만 있으므로 Step 2(상세보강) 붙을 때.

### 11-7. 확정 요약

| 항목 | 값 |
|---|---|
| base URL | `http://apis.data.go.kr/B554287/LocalGovernmentWelfareInformations` |
| 목록 / 상세 | `/LcgvWelfarelist` · `/LcgvWelfaredetailed` |
| 목록 파라미터 | `serviceKey` `pageNo` `numOfRows` `lifeArray=004` `searchWrd=자립준비청년` (srchKeyCode 없음) |
| 지역 순회 | 불필요 (전국 일괄 반환) |
| 판정 | `YouthClassifier` 재사용 — 좁게 조회라 전부 STRONG_YOUTH 수렴 |
