# Dday — 백엔드 컨벤션

## 1. 스택

| | |
|---|---|
| Spring Boot | 3.5.16 |
| Java / Gradle | 17 / 8.13 (wrapper) |
| 영속성 | Spring Data JPA (Hibernate 6) |
| DB | MySQL 8.4 |
| 검증 | Bean Validation |
| 문서 | springdoc-openapi 2.8.17 |
| 보조 | Lombok |

---

## 2. 패키지 구조

```
com.dday
├─ domain/{도메인}/
│  ├─ controller/
│  ├─ service/
│  ├─ repository/          Spring Data JPA 인터페이스
│  ├─ entity/              @Entity + 그 엔티티가 쓰는 enum
│  └─ dto/
│     ├─ request/
│     ├─ response/
│     ├─ {도메인}SuccessCode.java
│     └─ {도메인}ErrorCode.java
└─ global/                 이미 다 짜여 있다 — 손댈 일이 거의 없다
   ├─ common/{code,dto}/   SuccessCode, ApiResponse
   ├─ config/              WebConfig(CORS), SwaggerConfig
   ├─ exception/           ErrorCode, BusinessException, GlobalExceptionHandler
   └─ health/
```

- **`domain/pocket`이 참조 구현이다.** 새 도메인은 이걸 복제해서 시작
- **도메인 하나 = 폴더 하나.** 서로 다른 폴더를 만지면 충돌이 거의 없다

---

## 3. 네이밍

| 대상 | 규칙 | 예 |
|---|---|---|
| 패키지 | 소문자·단수 | `com.dday.domain.member` |
| 컨트롤러 · 서비스 · 레포지토리 | `{도메인}Controller` / `Service` / `Repository` | `MemberController` |
| 엔티티 | 단수 명사 | `Member` |
| 요청 DTO | `{동작}{대상}Request` | `MemberSignupRequest` |
| 응답 DTO | `{대상}{용도}Response` | `MemberListResponse` |
| 조회 조건 | `{대상}SearchCondition` | `PostSearchCondition` |
| 테스트 메서드 | 한글 + 언더스코어 | `이메일이_중복이면_예외를_던진다()` |

---

## 4. 엔티티 / JPA

| 규칙 | 안 지키면 |
|---|---|
| 엔티티에 **`@Setter` 금지**. 생성은 `@Builder`, 변경은 이름 있는 메서드 | 어디서 상태가 바뀌었는지 못 쫓는다 |
| `@ManyToOne`·`@OneToOne`은 **반드시 `fetch = LAZY`** | 기본값 EAGER — 조회 한 번에 테이블 대여섯 개가 딸려온다 |
| `@Enumerated(**EnumType.STRING**)` 명시 | 기본값 ORDINAL — enum 순서를 바꾸면 기존 데이터 의미가 어긋난다 |
| **양방향 연관관계를 기본으로 만들지 않는다.** 단방향 `@ManyToOne`으로 시작 | 정합성 맞추는 코드가 계속 늘어난다 |
| **컨트롤러에서 `@Entity`를 반환하지 않는다.** Response DTO로 변환 | 순환 참조 무한 루프 / 지연 로딩 프록시 직렬화 500 |
| 금액(원)은 **`Long`**, 소수가 필요한 값만 **`BigDecimal`**. `double`·`float`는 어디서도 금지 | 돈 계산이 틀어진다 |
| `createdAt`·`updatedAt`은 `@CreationTimestamp`·`@UpdateTimestamp` | |

### 금액 타입

**원 단위 정수는 `Long`을 쓴다.** 우리가 다루는 돈은 전부 원 단위라 소수점이 없다.
`BigDecimal`은 연산마다 `scale`을 맞춰야 하고 `equals`가 `compareTo`와 다르게 동작해서
(`1.0`과 `1.00`이 다른 값이 된다) 정수만 담을 거면 손해가 더 크다.

```java
private Long amount;            // ✅ 850000원
private BigDecimal quantity;    // ✅ 주식 0.5주 — 소수가 실제로 필요하다
private double amount;          // ❌ 부동소수점은 금액에 절대 쓰지 않는다
```

`BigDecimal`이 맞는 건 **소수가 실제로 의미 있는 값**뿐이다.

| 예 | 이유 |
|---|---|
| `InvestmentTransaction.quantity`·`unitPrice` | 소수점 주식·펀드 좌수 |
| `CreditBand.loanInterestRate`·`loanApprovalRate` | 이율·확률 (0.0~1.0) |

**예외는 없다.** 규칙이 `BigDecimal`이던 시절의 원 단위 컬럼
(`User.initialAsset`·`housingDeposit`, `WelfareProgram.supportAmount`, `CreditBand.cardMaxLimit`)은
전부 `Long`으로 옮겼다. 금액 컬럼에 `BigDecimal`이 보이면 그건 실수다.

> ⚠️ `ddl-auto: update`는 **컬럼 타입 변경을 반영하지 않는다.** `decimal` → `bigint`처럼
> 타입을 바꾸면 옛 컬럼이 그대로 남아 조용히 어긋난다. 로컬 DB를 새로 만들어야 한다 (§9).

**N+1**: 목록 API를 짤 때는 `join fetch`나 `@EntityGraph`를 쓴다.
`show-sql: true`가 켜져 있으니 **로컬에서 쿼리 개수를 눈으로 확인하고 넘어간다.**

특히 **`FinancialTransaction`은 `@ManyToOne`이 6개**(`account`·`card`·`counterpartyAccount`·
`originalTransaction`·`category`·`pocket`)라, 목록을 그냥 뽑으면 쿼리가 배수로 늘어난다.
`default_batch_fetch_size: 100`이 완화해줄 뿐 없애주지는 않는다.

---

## 5. DTO

Request·Response 모두 **class + Lombok**. `record`를 쓰지 않는다.

```java
// Request
@Getter
@NoArgsConstructor
public class MemberSignupRequest {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;
}

// Response
@Getter
@AllArgsConstructor
@Builder
public class MemberListResponse {
    private Long memberId;
    private String nickname;

    public static MemberListResponse from(Member member) { ... }
}
```

- **`@Setter` 금지** (양쪽 모두)
- **검증 어노테이션의 `message`를 반드시 채운다** — 이 문구가 그대로 프론트 폼에 뜬다.
  비우면 "must not be blank"가 사용자에게 노출된다
- `@ModelAttribute`로 GET 쿼리를 묶는 DTO만 예외적으로 `@Setter`가 필요하다

---

## 6. 응답 / 예외

모든 응답이 `ApiResponse<T>` 봉투에 담긴다. HTTP 상태코드도 의미대로 쓴다.

```json
{ "success": true,  "code": "MEMBERS_FOUND",     "message": "...", "data": [ ... ] }
{ "success": false, "code": "MEMBER_NOT_FOUND",  "message": "...", "data": null }
{ "success": false, "code": "INVALID_INPUT_VALUE", "message": "...", "data": null,
  "errors": [ { "field": "email", "reason": "이메일 형식이 올바르지 않습니다." } ] }
```

`errors`는 검증 실패일 때만 붙는다.

|  | 인터페이스 | 도메인별 enum |
|---|---|---|
| 성공 | `global/common/code/SuccessCode` | `domain/member/dto/MemberSuccessCode` |
| 실패 | `global/exception/ErrorCode` | `domain/member/dto/MemberErrorCode` |

```java
@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");

    private final HttpStatus status;
    private final String message;
}

// 컨트롤러
return ApiResponse.of(MemberSuccessCode.MEMBER_CREATED, memberService.signup(request));

// 서비스
throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
```

- **예외는 `BusinessException` 하나만.** 새 에러는 예외 클래스가 아니라 ErrorCode 상수를 추가
- **컨트롤러에서 try-catch 금지.** `GlobalExceptionHandler`가 전부 처리한다
- **도메인별 enum을 쓴다.** 전역 단일 enum은 5명이 붙으면 계속 충돌한다
- 401은 공통 `CommonErrorCode.UNAUTHORIZED`

### 계약의 정본은 코드다

프론트는 이 저장소를 직접 읽는다. **아래 둘은 프론트가 화면에 그대로 쓴다.**

- `{도메인}ErrorCode`의 `message` — 에러 문구
- 검증 어노테이션의 `message` — 폼 검증 문구

Swagger 어노테이션(`@Tag`·`@Operation`)은 여력 될 때만. 없어도 문서는 비지 않는다.

---

## 7. 트랜잭션

- **`@Transactional`은 Service에만.** Controller·Repository엔 붙이지 않는다
- **조회는 `@Transactional(readOnly = true)`**
- 서비스는 인터페이스 없이 클래스 하나로
- `open-in-view: false`라 **지연 로딩은 서비스 안에서 끝낸다.**
  컨트롤러에서 `LazyInitializationException`이 나면 "DTO 변환을 서비스로 옮기라"는 신호다

---

## 8. 테스트

커버리지를 강제하지 않는다. 두 가지만 지킨다.

- **`DdayApplicationTests`를 지우지 않는다.** 깨지면 서버가 안 뜬다는 뜻이다
- **핵심 로직에는 서비스 단위 테스트를 붙인다**

| 계층 | 도구 | DB |
|---|---|---|
| 서비스 단위 | Mockito (`@Mock` / `@InjectMocks`) | 불필요 |
| 컨트롤러 | MockMvc `standaloneSetup` | 불필요 |
| 컨텍스트 로딩 | `@SpringBootTest` | **필요** |

단언은 AssertJ `assertThat`으로 통일. Testcontainers를 두지 않는다.

---

## 9. 시드 데이터

`backend/src/main/resources/data.sql`이 **앱이 뜰 때마다 실행된다.**
로컬에서 띄우면 로컬 MySQL에, 서버에서 띄우면 RDS에 들어간다.
**두 DB를 동기화하는 게 아니라 이 파일 하나에서 양쪽이 각각 채워진다.**

```sql
-- 매 기동마다 돌므로 반드시 멱등하게. id를 박고 ON DUPLICATE KEY UPDATE.
INSERT INTO pocket (id, type, monthly_budget) VALUES (1, 'HOUSING', 650000.00)
ON DUPLICATE KEY UPDATE monthly_budget = VALUES(monthly_budget);
```

- **id를 auto-increment에 맡기지 않는다** — 매번 새 행이 생겨 멱등성이 깨진다
- **`CREATE TABLE` 금지.** 테이블은 엔티티가 만든다. 여기는 INSERT/UPDATE 전용
- **여기서 실패하면 앱이 안 뜬다** (의도된 동작)
- **대량 데이터(수만 행+)는 넣지 않는다.** 별도 적재 스크립트로 한 번만

**스키마를 바꾸면 시드를 다시 넣어야 할 수 있다.** `ddl-auto: update`는 추가만 하므로,
필드·테이블 **이름을 바꾸면 데이터가 옛 컬럼에 갇힌다.** 그때는 DB를 새로 만든다.

```bash
cd backend && docker compose down -v && docker compose up -d   # 로컬
# RDS: DROP DATABASE dday; CREATE DATABASE dday;
```

---

## 10. 환경 / 프로파일

```
src/main/resources/
├─ application.yml          공통 · 프로파일 기본값 local
├─ application-local.yml    docker compose 기준값
└─ application-prod.yml     환경변수 참조만
```

- **기본 프로파일은 `local`.** 그냥 띄우면 로컬로 뜬다 (CI도 이걸로 돈다)
- 운영은 `SPRING_PROFILES_ACTIVE=prod` + `DB_URL`·`DB_USERNAME`·`DB_PASSWORD`
- **커밋되는 파일에 운영 비밀번호를 넣지 않는다**

### compose 파일 두 개

**겹치는 서비스가 없다. 동기화할 게 없다는 뜻이다.**

| | `docker-compose.yml` (로컬) | `docker-compose.prod.yml` (서버) |
|---|---|---|
| MySQL | 컨테이너 | 없음 — RDS |
| 백엔드 | 없음 — `./gradlew bootRun` | 컨테이너 |

맞춰야 하는 건 **각 진영 안에서**다.

- 로컬: `docker-compose.yml` ↔ `.env.sample` ↔ `application-local.yml` ↔ `ci.yml`의 service container
  → **로컬 DB 비밀번호를 바꾸면 CI도 고쳐야 한다**
- 서버: `application-prod.yml`이 읽는 환경변수 *이름* ↔ `docker-compose.prod.yml`이 넘기는 이름

---

## 11. Git 워크플로우

저장소 전체에 적용되는 규칙이라 [CONTRIBUTING.md](../CONTRIBUTING.md)로 옮겼다.
`main`에 직접 push하지 않는다. 브랜치 → PR → Squash 머지.

---

## 12. 배포

```
main push (backend/** 변경 시)
  └─ build   컴파일 + 테스트
     └─ image   도커 이미지 → GHCR
        └─ deploy  EC2 SSH → compose pull && up -d → /health/db 확인
```

- 운영 DB는 **RDS**. 앱만 EC2 컨테이너로 뜬다
- **로컬 개발은 RDS에 붙지 않는다.** 5명이 같은 DB를 밟으면 서로 데이터를 지운다
- 운영 DB 접속정보는 **서버 `.env`에만** 있다 (저장소·GitHub Secrets에 없음)
- 배포 후 **`GET /health/db`를 확인한다.** `/health`는 DB 단절을 못 잡는다

---

## 부록 — 자주 밟는 함정

막혔을 때 여기부터 본다. 전부 실제로 한 번씩 겪은 것들이다.

| 증상 | 원인 · 해결 |
|---|---|
| `Table doesn't exist` (기동 시) | `data.sql`이 테이블보다 먼저 돌았다. `spring.jpa.defer-datasource-initialization: true` 확인 |
| `'script' must not be null or empty` | `data.sql`에 실행할 문장이 하나도 없다. 맨 위 `SELECT 1;`을 **지우지 말 것** |
| 재시작할 때마다 시드가 중복 | `data.sql`에 id를 안 박았다. `ON DUPLICATE KEY UPDATE` |
| 엔티티를 고쳤는데 컬럼이 안 바뀜 | `ddl-auto: update`는 **추가만** 한다. DB를 새로 만든다 (§9) |
| IDE로 실행하면 DB 접속 실패 | IDE는 `.env`를 안 읽는다. 실행 구성 환경변수에 `MYSQL_PORT` 추가 |
| 컨트롤러에서 `LazyInitializationException` | DTO 변환을 서비스 안으로 옮긴다 (§7) |
| 응답 JSON에 필드가 통째로 빠짐 | Response DTO에 `@Getter`가 없다 |
| `@ModelAttribute` DTO 필드가 전부 `null` | 그 DTO엔 `@Setter`가 필요하다 (§5) |
| `main`에 push가 거부됨 (`protected branch`) | 정상이다. 브랜치를 파서 PR로 올린다 ([CONTRIBUTING.md](../CONTRIBUTING.md)) |
| 실수로 `main`에 커밋해버렸다 | `git branch feat/작업이름` → `git reset --hard origin/main` → `git checkout feat/작업이름` |
| 프론트에서 CORS 에러 | 프론트가 절대 URL로 불렀다. `/api/...` 상대경로를 써야 한다 (`frontend/AGENTS.md`) |
