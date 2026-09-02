# Dday

26KB 해커톤(3일) 프로젝트. **프론트엔드와 백엔드가 한 저장소에 있는 모노레포**다.

```
Dday/
├─ backend/                 Spring Boot 3.5 + JPA (Java 17)
├─ frontend/                (스택 미정)
├─ docs/                    설계 메모 · 배포 가이드
├─ docker-compose.yml       로컬 개발용 MySQL
└─ docker-compose.prod.yml  서버 배포용 (MySQL + 백엔드)
```

- **코드 컨벤션과 작업 플로우 → [AGENTS.md](./AGENTS.md)** (읽고 시작할 것)
- **배포 → [docs/deploy.md](./docs/deploy.md)**

---

## 시작하기

필요한 것: **JDK 17**, **Docker Desktop**

```bash
git clone https://github.com/26KB-hackathon-Dday/Dday.git
cd Dday

cp .env.sample .env        # 최초 1회
docker compose up -d       # MySQL 기동 (빈 DB — 테이블은 앱이 뜰 때 JPA가 만든다)

cd backend
./gradlew bootRun
```

확인:

```bash
curl http://localhost:8080/health/db
# {"success":true,"code":"DB_HEALTHY","message":"데이터베이스 연결이 정상입니다.","data":1}
```

- API 문서(Swagger UI): http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### 3306 포트가 이미 쓰이고 있다면

`.env`의 `MYSQL_PORT`만 다른 값(예: `3307`)으로 바꾸고 `docker compose up -d`를 다시 돌린다.
`build.gradle`이 `.env`를 읽어 `bootRun`/`test`에 넘겨주므로 다른 파일은 안 고쳐도 된다.

> IDE에서 `DdayApplication`의 main을 직접 실행하는 경우엔 `.env`를 읽지 않는다.
> 실행 구성의 환경변수에 `MYSQL_PORT`를 넣어준다.

### DB가 이상해졌을 때

`ddl-auto: update`는 컬럼 추가만 반영하고 **삭제·타입 변경은 반영하지 않는다.**
엔티티를 크게 바꾼 뒤 스키마가 꼬이면 DB를 새로 만든다:

```bash
docker compose down -v && docker compose up -d
```

---

## 현재 들어 있는 것

백엔드는 **도메인 코드가 아직 없고, 모든 도메인이 공통으로 쓸 뼈대만** 있다.

| | |
|---|---|
| `ApiResponse<T>` | 모든 응답을 감싸는 봉투 (success / code / message / data) |
| `SuccessCode` · `ErrorCode` | 성공·실패 코드 enum 계약 |
| `BusinessException` | 이 프로젝트의 유일한 비즈니스 예외 |
| `GlobalExceptionHandler` | 모든 에러 응답을 여기서 만든다 |
| `WebConfig` | CORS (프론트 dev 서버 허용) |
| `SwaggerConfig` | springdoc 설정 |
| `HealthController` | `/health`, `/health/db` |

첫 도메인은 [AGENTS.md §5·§6](./AGENTS.md)의 코드 블록을 복사해서 시작하면 된다.

---

## 프론트엔드

`frontend/`는 아직 비어 있다. 스택이 정해지면 그 안에 프로젝트를 만들고
[frontend/README.md](./frontend/README.md)를 채운다.
CORS 허용 출처는 `backend/src/main/resources/application-local.yml`의
`app.cors.allowed-origins`에 있다 (기본으로 5173·3000이 열려 있다).
