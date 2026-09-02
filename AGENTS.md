# Dday

26KB 해커톤(3일) 프로젝트. **프론트엔드와 백엔드가 한 저장소에 있는 모노레포**다.

이 문서가 **코드 컨벤션과 작업 플로우의 정본**이다. `CLAUDE.md`는 이 파일을 가져다 쓴다.

```
Dday/
├─ backend/     Spring Boot 3.5 + JPA (Java 17)
├─ frontend/    (스택 미정 — 정해지면 여기에)
├─ docs/        설계 메모, 화면 기획, 회의록
└─ docker-compose.yml
```

> ⚠️ **3일짜리다.** 이 문서의 규칙은 "나중에 유지보수하기 좋으라고"가 아니라
> **5명이 같은 코드베이스를 동시에 만지면서 서로 안 깨뜨리려고** 있는 것이다.
> 여기에 없는 건 각자 편한 대로 해도 된다.

---

## 빌드 · 실행

```bash
cp .env.sample .env       # 최초 1회
docker compose up -d      # 로컬 MySQL (빈 DB. 테이블은 앱이 뜰 때 JPA가 만든다)

cd backend
./gradlew bootRun         # http://localhost:8080
./gradlew build           # 컴파일 + 테스트 — MySQL이 떠 있어야 한다
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- 헬스체크: `GET /health` (앱만) / `GET /health/db` (DB까지)

**3306이 이미 쓰이고 있으면** `.env`의 `MYSQL_PORT`만 바꾼다.
`build.gradle`이 `.env`를 읽어 `bootRun`/`test`에 넘겨주므로 다른 데는 안 고쳐도 된다.
단, **IDE에서 main 메서드를 직접 실행하면 `.env`를 안 읽는다** — 그때는 IDE 실행 구성의
환경변수에 `MYSQL_PORT`를 넣는다.

---

## 1. 기술 스택

| 항목 | 버전 | 비고 |
|---|---|---|
| Spring Boot | **3.5.16** | Boot 4가 아니다 (아래 참고) |
| Java | 17 | Gradle toolchain으로 고정 |
| Gradle | 8.13 | wrapper(`./gradlew`)를 쓴다. 로컬 gradle 버전은 상관없다 |
| 영속성 | Spring Data JPA (Hibernate 6) | |
| DB | MySQL 8.4 | docker compose |
| 검증 | Bean Validation (`spring-boot-starter-validation`) | |
| 문서 | springdoc-openapi 2.8.17 | |
| 보조 | Lombok | |

**왜 Boot 4.1이 아니라 3.5인가.** 3일 안에 막히면 검색으로 뚫어야 하는데,
지금 나오는 예제·블로그·스택오버플로 답의 대부분이 Boot 3 기준이다.
Boot 4는 Jackson 3(`tools.jackson.*`로 패키지가 통째로 이동)과 Spring Security 7(설정 DSL 변경)을
끌고 오기 때문에 **복붙한 코드가 컴파일은 되는데 런타임에 어긋나는** 상황이 생긴다.
해커톤에서 그걸 디버깅할 시간이 없다. 끝나고 올리고 싶으면 그때 올린다.

---

## 2. 패키지 / 도메인 구조

```
com.dday
├─ domain/{도메인}/
│  ├─ controller/
│  ├─ service/
│  ├─ repository/            # Spring Data JPA 인터페이스
│  ├─ entity/                # @Entity — 테이블과 1:1
│  └─ dto/
│     ├─ request/
│     ├─ response/
│     ├─ {도메인}SuccessCode.java
│     └─ {도메인}ErrorCode.java
└─ global/
   ├─ common/{code,dto}/     # SuccessCode, ApiResponse
   ├─ config/                # WebConfig(CORS), SwaggerConfig
   ├─ exception/             # ErrorCode, BusinessException, GlobalExceptionHandler
   └─ health/
```

**도메인 하나 = 폴더 하나**로 자른다. 5명이 붙어도 서로 다른 폴더를 만지면 충돌이 거의 없다.
공용으로 쓸 게 생기면 `global/`에 올리되, **올리기 전에 팀에 한마디 한다** — 여기가
유일하게 모두가 부딪히는 지점이다.

> `global/`은 이미 다 짜여 있다. 첫 도메인을 만들 때 §5의 코드 블록을 복사해서 시작하면 된다.

---

## 3. 네이밍 컨벤션

| 대상 | 규칙 | 예 |
|---|---|---|
| 패키지 | 소문자·단수 | `com.dday.domain.member` |
| 컨트롤러/서비스/레포지토리 | `{도메인}Controller` / `Service` / `Repository` | `MemberController` |
| 엔티티 | 단수 명사 | `Member`, `Post` |
| 요청 DTO | `{동작}{대상}Request` | `MemberSignupRequest` |
| 응답 DTO | `{대상}{용도}Response` | `MemberListResponse` |
| 조회 조건 | `{대상}SearchCondition` | `PostSearchCondition` |
| DB → 필드 | `snake_case` → `camelCase` (Hibernate 기본 전략이 알아서) | `created_at` → `createdAt` |
| 테스트 메서드 | 한글 + 언더스코어 | `회원_가입시_이메일이_중복이면_예외를_던진다()` |

---

## 4. 엔티티 / JPA 규칙

지키지 않으면 **다른 사람 코드까지 같이 터지는** 것들만 모았다.

- **엔티티에 `@Setter`를 붙이지 않는다.** 아무 데서나 상태가 바뀌면 버그를 못 쫓는다.
  생성은 `@Builder`, 변경은 의미 있는 이름의 메서드(`changeNickname(...)`)로 한다
- **`@ManyToOne`·`@OneToOne`은 반드시 `fetch = FetchType.LAZY`.**
  기본값이 EAGER라서 안 적으면 조회 한 번에 테이블 대여섯 개가 딸려온다
- **양방향 연관관계를 기본으로 만들지 않는다.** 단방향 `@ManyToOne`으로 시작하고,
  반대 방향이 정말 필요할 때만 `@OneToMany`를 추가한다
- **`@Entity`를 컨트롤러에서 반환하지 않는다.** 반드시 Response DTO로 바꿔서 내보낸다.
  엔티티를 그대로 직렬화하면 ① 양방향 참조에서 무한 루프가 나고 ② 지연 로딩 프록시를
  Jackson이 건드려서 `open-in-view: false`와 함께 500이 난다
- **금액은 `BigDecimal`.** `double`/`float` 금지 — 0.1 + 0.2 문제가 돈에서 터진다
- **`createdAt`/`updatedAt`은 `@CreationTimestamp`/`@UpdateTimestamp`**로 붙인다. 수동으로 넣지 않는다
- **`ddl-auto: update`**로 돌고 있다. 엔티티를 고치면 스키마가 따라오지만,
  **컬럼 삭제와 타입 변경은 반영되지 않고 옛 컬럼이 조용히 남는다.**
  이상해지면 `docker compose down -v && docker compose up -d`로 DB를 새로 만든다

### N+1

목록을 뽑고 그 안에서 연관 엔티티를 건드리면 쿼리가 행 수만큼 더 나간다.
`application.yml`의 `default_batch_fetch_size: 100`이 최악은 막아주지만, 목록 API를 짤 때는
`@Query`에 `join fetch`를 쓰거나 `@EntityGraph`를 붙이는 게 정석이다.
**`show-sql: true`가 켜져 있으니 로컬에서 쿼리 개수를 눈으로 확인하고 넘어간다.**

---

## 5. DTO 규칙

Request·Response 모두 **class + Lombok**으로 쓴다 (`record`를 쓰지 않는다 — 팀이 익숙한 쪽으로 통일).

```java
// Request
@Getter
@NoArgsConstructor
public class MemberSignupRequest {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.")
    private String nickname;
}

// Response
@Getter
@AllArgsConstructor
@Builder
public class MemberListResponse {
    private Long memberId;
    private String nickname;

    public static MemberListResponse from(Member member) {
        return MemberListResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .build();
    }
}
```

- **`@Setter` 금지** (양쪽 모두)
- **검증 어노테이션의 `message`를 반드시 채운다.** 이 문구가 그대로 프론트 폼에 뜬다.
  비워두면 "must not be blank" 같은 영문 기본 문구가 사용자에게 노출된다
- **GET 쿼리 파라미터를 객체로 묶을 때(`@ModelAttribute`)만 예외적으로 `@Setter`가 필요하다.**
  안 붙이면 예외 없이 필드가 전부 `null`로 조용히 남는다. 그 DTO에만 붙이고 주석을 남긴다

---

## 6. 응답 포맷 / 예외 처리

모든 응답은 `ApiResponse<T>` 봉투에 담긴다. HTTP 상태코드도 의미대로 쓴다.

```json
// 성공
{ "success": true, "code": "MEMBERS_FOUND", "message": "회원 목록을 조회했습니다.", "data": [ ... ] }

// 실패 — data는 기본적으로 null
{ "success": false, "code": "MEMBER_NOT_FOUND", "message": "회원을 찾을 수 없습니다.", "data": null }

// 검증 실패 — errors가 추가된다 (이때만 나온다)
{ "success": false, "code": "INVALID_INPUT_VALUE", "message": "입력값이 올바르지 않습니다.",
  "data": null, "errors": [ { "field": "email", "reason": "이메일 형식이 올바르지 않습니다." } ] }
```

성공·실패 모두 `code`와 `message`를 갖고, **양쪽 다 enum**으로 관리한다.
문자열을 컨트롤러에 흩뿌리지 않기 위해서다.

|  | 인터페이스 | 도메인별 enum |
|---|---|---|
| 성공 | `global/common/code/SuccessCode` | `domain/member/dto/MemberSuccessCode` |
| 실패 | `global/exception/ErrorCode` | `domain/member/dto/MemberErrorCode` |

```java
// 도메인 성공 코드
@Getter
@RequiredArgsConstructor
public enum MemberSuccessCode implements SuccessCode {

    MEMBERS_FOUND(HttpStatus.OK, "회원 목록을 조회했습니다."),
    MEMBER_CREATED(HttpStatus.CREATED, "회원 가입이 완료되었습니다.");

    private final HttpStatus status;
    private final String message;
}

// 도메인 에러 코드
@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");

    private final HttpStatus status;
    private final String message;
}

// 컨트롤러 — 성공 응답
@PostMapping("/api/members")
public ResponseEntity<ApiResponse<MemberListResponse>> signup(
        @Valid @RequestBody MemberSignupRequest request) {
    return ApiResponse.of(MemberSuccessCode.MEMBER_CREATED, memberService.signup(request));
}

// 서비스 — 실패
throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
```

- `code`는 enum 상수 이름(`name()`) 그대로 나간다
- **전역 단일 enum을 쓰지 않는다** — 5명이 병렬로 개발하면 그 파일에서 계속 충돌한다
- **예외는 `BusinessException` 하나만 쓴다.** 새 에러는 예외 클래스가 아니라 ErrorCode 상수를 추가한다
- **컨트롤러에서 try-catch로 에러 응답을 만들지 않는다.** `GlobalExceptionHandler`가 전부 처리한다
- 401은 도메인 ErrorCode를 만들지 말고 공통 `CommonErrorCode.UNAUTHORIZED`를 쓴다

### API 계약의 정본은 코드다

**프론트는 이 저장소를 직접 읽는다.** 컨트롤러·DTO·`ErrorCode` enum이 곧 API 문서다.
Swagger는 경로 목록을 훑는 보조 수단이며 정본이 아니다.

그래서 아래 두 가지는 **프론트가 그대로 읽는다는 전제로** 쓴다. 어차피 써야 하는 것들이라
추가 부담이 없고, 손으로 베낀 사본과 달리 어긋나지 않는다.

- **`{도메인}ErrorCode`의 `message`** — 화면에 띄울 에러 문구의 정본
- **검증 어노테이션의 `message`** — 폼 검증 문구의 정본

Swagger 어노테이션(`@Tag`, `@Operation`, `@Schema`)은 **여력이 될 때만** 붙인다.
없어도 springdoc이 메서드명·파라미터명으로 채우므로 문서가 비지 않는다. 리뷰에서 막지 않는다.

---

## 7. 트랜잭션 경계

- **`@Transactional`은 Service에만** 붙인다. Controller와 Repository에는 붙이지 않는다
- **조회 메서드는 `@Transactional(readOnly = true)`**
- `open-in-view: false`이므로 **지연 로딩은 서비스(트랜잭션) 안에서 끝낸다.**
  컨트롤러에서 프록시를 건드리면 `LazyInitializationException`이 난다 —
  버그가 아니라 "DTO 변환을 서비스에서 하라"는 신호다
- 서비스는 인터페이스 없이 클래스 하나로 만든다. 구현체가 하나뿐인데 인터페이스를 두면
  3일 동안 파일만 두 배가 된다

---

## 8. 테스트

해커톤이라 **커버리지를 강제하지 않는다.** 대신 두 가지만 지킨다.

- **`DdayApplicationTests`(컨텍스트 로딩 테스트)를 절대 지우지 않는다.**
  이게 깨졌다는 건 서버가 안 뜬다는 뜻이고, CI가 그걸 잡아준다
- **핵심 비즈니스 로직에는 서비스 단위 테스트를 붙인다** —
  `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks`, DB 불필요

| 계층 | 도구 | DB |
|---|---|---|
| 서비스 단위 | Mockito | 불필요 |
| 컨트롤러 | MockMvc `standaloneSetup` + Mock Service | 불필요 |
| 컨텍스트 로딩 | `@SpringBootTest` | **필요** (docker compose mysql) |

- 단언은 **AssertJ `assertThat`으로 통일**한다
- 테스트 DB는 docker compose MySQL을 그대로 쓴다. Testcontainers를 두지 않는다

---

## 9. 환경 / 프로파일

```
backend/src/main/resources/
├─ application.yml          # 공통. 프로파일 기본값 local
├─ application-local.yml    # 로컬. docker compose 기준값
└─ application-prod.yml     # 서버. 값 자리에 환경변수 참조만
```

- **프로파일 기본값은 `local`.** 지정하지 않고 띄우면 로컬로 뜬다 (CI도 이 기본값으로 돈다)
- 운영은 `SPRING_PROFILES_ACTIVE=prod` + `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 환경변수.
  없으면 기동 단계에서 실패한다 (의도된 동작)
- **커밋되는 파일에 운영 비밀번호를 넣지 않는다.** `.env`는 `.gitignore`에 있다

---

## 10. Git / GitHub 워크플로우

**브랜치는 `main` 하나다.** `develop`을 두지 않는다 — 3일짜리에 브랜치를 두 갈래로 관리하면
머지 비용이 개발 시간을 잡아먹는다.

```bash
# 푸시하기 전에 항상 rebase로 당겨온다. merge로 당기면 커밋 그래프가 금방 엉킨다.
git pull --rebase origin main
git push origin main
```

- 커밋: `type: 한국어 설명` — `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `style`, `ci`
- **여러 명이 같은 파일을 만질 게 뻔한 큰 작업**은 `{type}/{설명}` 브랜치를 파서 PR로 올린다.
  리뷰 승인을 기다리지 말고 CI만 green이면 셀프 머지해도 된다 — 목적은 승인이 아니라
  "누가 뭘 건드리는 중인지" 보이게 하는 것이다
- 작은 변경은 `main`에 바로 push해도 된다. 대신 **push 전에 `./gradlew build`를 돌린다**

### 이슈

큰 덩어리만 이슈로 남긴다. 제목은 `[TYPE] 한국어 설명`.

| 접두사 | 라벨 |
|---|---|
| `[FEAT]` | `✨ 기능` |
| `[BUG]` | `🐛 버그` |
| `[TASK]` | `🛠️ 작업` |
| `[DOCS]` | `📝 문서` |

---

## 11. 배포

**main에 push하면 GitHub Actions가 자동으로 배포한다.**
자세한 절차와 EC2 세팅은 [docs/deploy.md](./docs/deploy.md)에 있다.

```
main push
  └─ build   컴파일 + 테스트 (MySQL service container)
     └─ image   도커 이미지 빌드 → GHCR push
        └─ deploy  EC2에 SSH → docker compose pull && up -d → /health/db 확인
```

- **`deploy` 잡은 저장소 Variable `DEPLOY_ENABLED=true`일 때만 돈다.**
  EC2를 아직 안 만들었으면 통째로 건너뛰므로 main push가 빨갛게 뜨지 않는다
- 배포 후 항상 `GET /health/db`를 확인한다 — 앱은 떠 있는데 DB에 못 닿는 경우를
  `/health`는 못 잡는다
- 롤백은 EC2에서 이미지 태그를 이전 커밋 sha로 바꿔 다시 올린다 (docs/deploy.md 참고)
- **RDS를 쓰지 않는다.** DB도 같은 EC2에 컨테이너로 뜬다. 비용 때문이 아니라
  세팅 시간과 실패 지점을 줄이려는 결정이고, 바꿔야 할 조건과 방법은 docs/deploy.md에 있다.
  대가는 두 가지다 — ① 인스턴스를 terminate하면 데이터도 사라진다
  ② MySQL과 JVM이 한 박스의 메모리를 나눠 쓴다(`mem_limit`으로 미리 갈라놨다)
