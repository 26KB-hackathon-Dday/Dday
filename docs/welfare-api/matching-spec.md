# 지원금 매칭 API 명세

전체 설계 명세(도메인 모델·SUBSIDY-001~008)는 Notion에 있다:
<https://app.notion.com/p/API-3cecbbe06288812fb6adc93f5f7c6221> (2026-09-03 기준).

**이 파일은 "구현이 확정된 것"만 담는다.** 코드가 계약의 정본이고(AGENTS.md §5),
Notion은 설계 원본이다. 새 엔드포인트를 구현하기 시작하면 그 확정본을 여기로 옮긴다.

수집·판정 규칙은 [welfare-collector.md](../welfare-collector.md), [NOTES.md](./NOTES.md) 참고.

---

## 공통

| 항목 | 값 |
|---|---|
| Base URL | `/api/v1` |
| 응답 봉투 | 전부 `ApiResponse<T>` — `{ success, code, message, data }` (global/common/dto) |
| 인증 | `/me/**` 는 `Authorization: Bearer {accessToken}` 필요. **아직 미구현** — 공개 엔드포인트(`/welfare-programs/**`)부터 구현 중 |
| 페이지네이션 | `?page=0&size=20` (Spring `Pageable`), 응답은 `PageResponse<T>` |
| 날짜 | ISO-8601 (`YYYY-MM-DD`) |

---

## SUBSIDY-009. 전체 지원제도 검색·목록 (신규 — Notion 명세 보강)

> Notion 명세엔 "놓치고 있는 제도"(SUBSIDY-004, `/me` 자격 기반)만 있고, 자격과 무관하게
> **전체 제도를 검색·탐색**하는 엔드포인트가 없다. `/grants/all` 화면(검색창 + 카테고리 칩)이
> 이 엔드포인트를 쓴다.
>
> - **검색(`q`)은 전체 제도 대상.** 자격 판별과 무관.
> - **카테고리(`category`)는 검색과 독립된 별개 축.** 둘 다 주면 AND.
> - 공개 데이터(제도 마스터)라 인증 불필요 — `/me`가 아닌 `/welfare-programs`.

### `GET /api/v1/welfare-programs`

Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `q` | string | X | 검색어. `servNm`·`jurMnofNm`·`targetDescription` 부분일치(대소문자 무시). 빈 값이면 전체 |
| `category` | string | X | 카테고리 정확일치 (`주거`·`생활`·`서민금융`·`교육`·`일자리`·`기타` …). 없으면 전체 |
| `page` | int | X | 0-base, 기본 0 |
| `size` | int | X | 기본 20 |

Response `200 OK` — code `WELFARE_PROGRAMS_FOUND`

```json
{
  "success": true,
  "code": "WELFARE_PROGRAMS_FOUND",
  "message": "지원제도 목록을 조회했습니다.",
  "data": {
    "content": [
      {
        "programId": "WLF00004661",
        "name": "청년 월세 특별지원",
        "category": "주거",
        "benefitText": "월 200,000원",
        "periodText": "~ 2026.12.31 마감",
        "status": "OPEN"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "last": false
  }
}
```

- `benefitText` / `periodText` / `status` 는 서버가 `supportAmount`·`supportAmountType`·
  `applicationDeadline`·`ongoingApplication` 에서 **파생**한다 (프론트가 각자 계산하지 않도록).
- `status`: `ongoingApplication == true` 이거나 마감일이 없거나 오늘 이후 → `OPEN`, 아니면 `CLOSED`.

### `GET /api/v1/welfare-programs/{programId}` (= SUBSIDY-005 상세)

`programId` 는 `servId` (예: `WLF00004661`).

Response `200 OK` — code `WELFARE_PROGRAM_FOUND`

```json
{
  "data": {
    "programId": "WLF00004661",
    "name": "청년 월세 특별지원",
    "agency": "국토교통부",
    "category": "주거",
    "targetDescription": "만 19세 ~ 34세 무주택 청년",
    "supportAmount": 200000,
    "supportAmountType": "MONTHLY",
    "supportDurationMonths": 12,
    "applicationDeadline": "2026-12-31",
    "ongoingApplication": false,
    "benefitText": "월 200,000원",
    "benefitNote": "최대 12개월간 지원",
    "periodText": "~ 2026.12.31 마감",
    "status": "OPEN",
    "incomeChange": {
      "title": "12개월 간 예상 수입 변화",
      "monthlyAmount": 200000,
      "durationMonths": 12
    },
    "requiredDocuments": ["임대차계약서", "소득금액증명원", "가족관계증명서"],
    "applicationChannel": { "name": "복지로", "url": "https://www.bokjiro.go.kr", "phone": "1600-0000" }
  }
}
```

### `incomeChange` — "예상 수입 변화" 카드 게이팅

`support_type == CASH && support_amount_type == MONTHLY && support_amount != null` 일 때만
`incomeChange` 객체가 채워진다. 그 외에는 `null` → 프론트가 카드를 숨긴다.

| support_type | support_amount_type | 상세 카드 |
|---|---|---|
| CASH | MONTHLY | **`incomeChange`** (예상 수입 변화) |
| CASH | FIXED (1회성) | 없음 (후속: 총액 카드 검토) |
| CASH | LIMIT / SEMIANNUAL | 없음 (후속 정의) |
| VOUCHER / SERVICE | — | 없음 |
| LOAN | — | 없음 (후속: 이자·상환 카드) |

- `incomeChange`는 **제도 사실**(월 지원액·개월 수)만 담는다.
- "현재 / 수령 시" 절대 금액(= 유저 월소득 ± 지원액)은 마이데이터가 필요해 **SUBSIDY-006**
  (`GET /me/subsidies/{programId}/pocket-impact`)에서 채운다. 이번 슬라이스 범위 아님.
- `durationMonths == null`(상시 지급)이면 `title`이 `"매월 예상 수입 변화"`가 된다.

Error

| HTTP | code | 상황 |
|---|---|---|
| 404 | `PROGRAM_NOT_FOUND` | 존재하지 않는 `programId` |

---

## 구현 현황

| 요구사항 | 엔드포인트 | 상태 |
|---|---|---|
| SUBSIDY-009 | `GET /api/v1/welfare-programs` | 🚧 진행 중 (이번 슬라이스) |
| SUBSIDY-005 | `GET /api/v1/welfare-programs/{programId}` | 🚧 진행 중 (이번 슬라이스) |
| SUBSIDY-001 | `POST /admin/welfare-programs/sync` | 배치([welfare-collector.md](../welfare-collector.md))가 사실상 수행. REST 래핑 미구현 |
| SUBSIDY-002 | `GET /me/subsidies/eligibility` | 미구현 (인증 + 자격판별 엔진 필요) |
| SUBSIDY-003 | `PATCH /me/subsidies/{programId}/receiving-status` | 미구현 |
| SUBSIDY-004 | `GET /me/subsidies/missed` | 미구현 |
| SUBSIDY-006 | `GET /me/subsidies/{programId}/pocket-impact` | 미구현 |
| SUBSIDY-007 | `GET /me/subsidies/deadline-alerts` | 미구현 |
| SUBSIDY-008 | `.../favorite` | 미구현 |

## Notion "WelfareProgram DB 스키마" ↔ 코드 (재설계 방향: 옵션 A)

<https://app.notion.com/p/WelfareProgram-DB-3cecbbe06288811d893bf1106804fd21>

전면 재설계(옵션 A)를 단계적으로 흡수한다. 진행 상황:

- ✅ **PK를 UUID로** (`id` `Long` auto_increment → `UUID` VARCHAR(36)). `servId`는 그대로 업무
  식별자(Notion의 `program_id`) 역할. 코드 어디서도 숫자 PK를 참조하지 않아 파급 없었음.
- 🔜 `eligibility_criteria` / `application_channel` / `required_documents` / `detection_params`
  → **JSON 컬럼**. 지금은 `requiredDocuments`(`|` 구분 문자열), `applyChannel*`(flat 3컬럼).
- 🔜 `detection_strategy` enum(RECURRING_DEPOSIT …), `verified_at`, `source_synced_at`,
  `agency` 컬럼 추가.
- 부분 ✅ `region_code` VARCHAR(10). `RegionCodeResolver`가 `ctpvNm`/`sggNm`에서 파생 —
  **v1은 시도 2자리만**(`11`=서울). CENTRAL/전국은 `null`. 시도명을 못 풀면 `null` +
  `WelfareIssue.REGION_UNRESOLVED`로 리뷰 큐. 시군구 5자리 정밀도·유저 거주지 대조는
  자격 매칭 붙일 때. (2026-07-01 광주+전남 → 전남광주통합특별시 29)
- 부분 ✅ enum 값 정리: `source`에 `MANUAL_CURATION` 추가(리뷰 큐 동결용) 완료. 나머지 🔜 —
  `support_type`에서 `LOAN` 제거, `support_amount_type`에서 `SEMIANNUAL` 제거,
  `support_amount` DECIMAL → BIGINT, `name` NOT NULL.
- ✅ 검증·리뷰: `curation_status`(`OK`/`NEEDS_REVIEW`)·`curation_issues` 컬럼, 수집 잡 Step 4가
  채움. `NEEDS_REVIEW`는 공개 API에서 제외(`isPubliclyVisible()`). 리뷰 큐 조회
  `GET /internal/welfare/review-queue`(local). 관리자가 DB에서 고치고 `source=MANUAL_CURATION`으로
  바꾸면 재수집이 큐레이션 필드를 안 덮고(`collected_at`·원문만 갱신) 큐에서도 빠진다.
- 🔜 수집 배치 raw 필드(`serv_dgst`·`life_array`·`rule_score`·`raw_*_xml` 등 22개) → 별도
  `welfare_program_raw` staging 분리 + 수집 배치 수정.
- ✅ `protection_phase` enum(`PRE_TERMINATION` 보호 중 아동 / `POST_TERMINATION` 자립준비청년 /
  `BOTH`). `ProtectionPhaseClassifier`가 지원대상·제도명 키워드로 채운다 — "자립준비청년"·"보호종료"
  → `POST_TERMINATION`, "보호대상아동"·"보호 중인 아동" → `PRE_TERMINATION`, 그 외 `null`(일반 대상).
  상세보강 직후 + 매 수집 잡 Step 4가 전 행에 재파생(분류기 개선 시 기존 행도 따라오도록).
  키워드 규칙은 LLM 분류(§6.1 등급 / `slctCritCn` 파싱)로 가는 다리. 수집셋 18건에서 자립 6건
  정확히 6건, 오탐 0.
  <br>※ 이 필드는 "지원금 매칭 API 명세서"에만 있고 "WelfareProgram DB 스키마" 문서엔 없다 — 두 문서 정합 필요.
- **`classificationTier`(STRONG / CHILD_POSITIVE / ADJACENT)** ↔ 기존 `YouthStatus`(STRONG_YOUTH /
  AUTO_APPROVED / REVIEW_QUEUE). enum을 하나로 합쳐야 한다. `applicantType`(YOUTH_SELF/GUARDIAN/
  INSTITUTION) 도 이때 같이.

## 유저 × 제도 테이블 (`user_program_eligibility` / `user_program_status`)

<https://app.notion.com/p/user_program_eligibility-user_program_status-3cfda81c6176807bae69efc6ad62caae>

자격(시스템 소유)과 수급여부·즐겨찾기(사용자 소유)를 **한 테이블에 안 넣고 2개로 분리** — 자격
재계산 배치가 UPDATE 칠 때 사용자 입력을 지우는 사고를 막는다.

- **✅ 엔티티·레포지토리 생성** (`com.dday.domain.welfare.entity` / `.repository`).
  복합키 `@EmbeddedId UserProgramId(userId, programId)`를 두 엔티티가 공유.
  - `user_program_eligibility` — `eligible`, `matched_criteria`(JSON), `ineligible_reason`,
    `evaluated_at`. `applyEvaluation(...)`으로만 갱신 (SUBSIDY-002 배치). `idx_user_eligible`.
  - `user_program_status` — `receiving_status`(enum, 기본 UNKNOWN), `detection_source`,
    `detection_evidence`(JSON), `is_favorite`, `favorited_at`. `updateReceiving(...)`/`setFavorite(...)`
    으로만 갱신 (SUBSIDY-003/008 사용자 API). `idx_user_receiving`, `idx_user_favorite`.
- **FK 없음 (의도).** `user_id`는 `user`/`member` 테이블이 아직 없어 soft reference (UUID 컬럼).
  `program_id`는 `welfare_program.serv_id`(UNIQUE)를 논리적으로 가리키지만, WelfareProgram 스키마가
  재설계 중이라 DB FK 대신 애플리케이션 레벨 조인. 정합성은 서비스 계층 책임 — `welfare_program`
  자체도 FK가 하나도 없다(프로젝트 컨벤션).
- 인증(SUBSIDY-001)이 들어오면: soft 유지(권장 — `ddl-auto: update`는 기존 테이블에 FK를 못 붙임)
  하거나, 준비되면 `ALTER TABLE ... ADD CONSTRAINT fk_...` 일회성 스크립트로 추가.
- 서비스·컨트롤러·DTO는 SUBSIDY-002/003/004/008과 함께 (인증·자격판별 엔진·마이데이터 필요).

## 매칭 필드 채우기 (수집 배치)

`welfare_program`의 매칭 API용 필드를 수집 시점에 파생한다.

| 필드 | 출처 | 방법 | 상태 |
|---|---|---|---|
| `category` | 목록 API `intrsThemaArray` + 제도명 | `WelfareCategoryClassifier` — ① 주제에 `주거` → 주거, ② 제도명에 적금·계좌·공제·저축·통장 → 자산형성, ③ 생활계열/`서민금융` 주제 → 생활, ④ 그 외 → 기타 | ✅ |
| `supportAmountType` | 목록 API `sprtCycNm` | `SupportCycleMapper` — 월→MONTHLY, 1회성→FIXED, 반기→SEMIANNUAL, 수시/기타→null | ✅ |
| `supportType` | 목록 API `srvPvsnNm`(급여형태) | `SupportTypeMapper` — 현금지급→CASH, 융자→LOAN, 서비스→SERVICE, 이용권→VOUCHER | ✅ |
| `supportAmount` (숫자) | 상세 API `alwServCn`(지원내용) — 자연어 | `SupportAmountParser` 정규식. **CASH만** 파싱(대출·서비스 금액은 성격이 달라). MONTHLY면 "월 N원"을 총액보다 우선, "시간당"은 무시 | ✅ (best-effort) |
| `supportDurationMonths` | 상세 `alwServCn` | "N개월"/"N년"(×12) 패턴 | ✅ (best-effort) |
| `crtrYr` | 상세 `crtrYr` (중앙만) | 그대로 | ✅ |
| `targetDescription` | 상세 `tgtrDtlCn`(중앙) / `sprtTrgtCn`(지자체) | `DetailText.cleanTarget` — 줄바꿈·중복공백만 접고 500자 컷. 각주 절단은 제거(표본 과적합, 형태 다른 원문에서 자격요건 유실) | ✅ 원문 통과 |
| `applicationChannel` `{name, phone, url}` | 상세 `inqplCtadrList`(문의처) 첫 항목 + `inqplHmpgReldList`(홈페이지) 첫 항목. 중앙/지자체 서브필드명 다름(`servSeDetailNm` / `wlfareInfoReldNm`). URL 스킴 없으면 `DetailText.normalizeUrl`이 `https://` 부착 | ✅ (전 제도) |
| `requiredDocuments` | 상세에 구조화 필드 없음 (`applmetList`=신청절차 텍스트만) | LLM 필요 | 🔜 |
| `applicationDeadline` | 목록·중앙상세에 없음. 텍스트에만 | 🔜 |
| `protectionPhase` | 지원대상·제도명 키워드 — "자립준비청년"·"보호종료" → `POST_TERMINATION`, "보호대상아동"·"보호 중인 아동" → `PRE_TERMINATION`, 그 외 `null` | `ProtectionPhaseClassifier`. 상세보강 직후 + Step 4가 전 행 재파생. LLM 분류로 가는 다리 | ✅ (키워드 v1) |
| `regionCode` | 목록 API `ctpvNm`(시도명) → 법정동 2자리 코드. `sggNm`은 v1에서 미사용 | `RegionCodeResolver` (시도명↔코드 표 20행). 못 풀면 `null` + `REGION_UNRESOLVED`. 수집 시 + Step 4 재파생 | ✅ (시도 단위 v1) |

정규화 카테고리 값: `주거` · `생활` · `자산형성` · `기타` (프론트 칩 = 전체 + 이 4개).
자립준비청년 대상 제도는 대부분 "생활 유지"(수당·학자금·취업)라 `생활`이 큰 통이 된다.
`intrsThemaArray`가 없으면(위기청년 전담 등) `기타`.

**상세보강 Step `enrichDetailStep` (구현됨):** 저장된 제도 전부(`raw_detail_xml IS NULL`) 상세 API 호출
→ `targetDescription`·`applicationChannel`은 전 제도, `supportAmount`·`supportDurationMonths`는
CASH만, `crtrYr`·`raw_detail_xml` 저장. `raw_detail_xml`이 채워지면 다음 실행에서 제외(멱등).
[welfare-collector.md](../welfare-collector.md) Step 3 참고. `requiredDocuments`는 상세에 구조화
필드가 없어 미구현(LLM 필요).

파싱 한계: "1인당 최대 1,350만원"(해외취업 — 여러 트랙 묶음), "본인저축액 10만원"(내일저축계좌 —
납입액을 지원액으로 오인) 같은 건 틀린다. LLM 추출·낮은신뢰도 관리자 큐는 §6.3.

**품질 검증 Step `reportQualityStep` (구현됨):** 저장분 전체를 `WelfareProgramValidator`로 훑어
`curation_status`(`OK`/`NEEDS_REVIEW`)·`curation_issues`에 기록하고 집계를 잡 로그로 남긴다.
레코드마다 규칙을 늘리는 대신 "몇 건이, 어떤 이유로" 리뷰가 필요한지 집계로 본다. 항목:
`CATEGORY_MISSING` / `CASH_WITHOUT_AMOUNT` / `AMOUNT_SUSPICIOUS`(월>1천만·총>2억·<1만) /
`TARGET_MISSING` / `TARGET_LOOKS_LIKE_NOTICE`(`※` 시작 or 앞머리 접수기간류 단어) / `CHANNEL_MISSING`.
`NEEDS_REVIEW` → 공개 목록·상세 API에서 제외, 상세 직접 접근은 404. 규칙은
`WelfareProgram#isPubliclyVisible()`(검증 전 `null`은 노출, `MANUAL_CURATION`은 항상 노출).

**리뷰 큐 (구현됨):** `GET /internal/welfare/review-queue`(local 프로파일) — `NEEDS_REVIEW`이고
아직 `source = API_CANDIDATE`인 행을, 걸린 사유·문제 필드 현재 값·`detailLink`(복지로 원본)와 함께.
관리자는 DB에서 값을 고친 뒤 `source = 'MANUAL_CURATION'`으로 바꾼다 →
① 재수집이 큐레이션 필드를 안 덮음(`applyCollection`/`applyDetail`이 `collected_at`·원문 XML만 갱신)
② 리뷰 큐에서 빠짐 ③ 검증에 걸려도 공개 API엔 계속 노출. 되돌리려면 `'API_CANDIDATE'`로.
쓰기 엔드포인트·강제 재보강·LLM 교정은 아직.
