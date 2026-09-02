# Dday

26KB 해커톤(3일) 프로젝트. **프론트엔드와 백엔드가 한 저장소에 있는 모노레포**다.

```
Dday/
├─ backend/     Spring Boot 3.5 + JPA  →  AWS EC2 (도커) + RDS
├─ frontend/    Vue 3 + TypeScript      →  Cloudflare Workers
└─ docs/        설계 메모 · 배포 가이드
```

**한쪽만 고치면 한쪽만 배포된다.** `backend/**`가 바뀌면 GitHub Actions가,
`frontend/**`가 바뀌면 Cloudflare Workers Builds가 각각 움직인다.
프론트 한 줄 고쳤다고 백엔드가 재배포되지 않는다.

**컨벤션 문서는 각 영역 안에 둔다.** 백엔드 규칙은 `backend/AGENTS.md`,
프론트엔드 규칙은 나중에 `frontend/AGENTS.md`. 에이전트(Claude Code · Codex)는
작업 중인 파일에서 가장 가까운 문서를 읽으므로, 백엔드를 고칠 땐 백엔드 규칙만 걸린다.

- **백엔드 컨벤션 → [backend/AGENTS.md](./backend/AGENTS.md)**
- **프론트엔드 컨벤션 → [frontend/AGENTS.md](./frontend/AGENTS.md)** — API 호출 규칙은 꼭 읽을 것
- **배포 → [docs/deploy.md](./docs/deploy.md)**

**배포된 API**: http://3.36.106.81 ([헬스체크](http://3.36.106.81/health/db) · [Swagger](http://3.36.106.81/swagger-ui.html))
main에 push하면 자동으로 갱신된다.

---

## 시작하기

필요한 것: **JDK 17**, **Docker Desktop**

```bash
git clone https://github.com/26KB-hackathon-Dday/Dday.git
cd Dday/backend            # 백엔드 작업은 전부 이 안에서 한다

cp .env.sample .env        # 최초 1회
docker compose up -d       # MySQL 기동 (빈 DB — 테이블은 앱이 뜰 때 JPA가 만든다)
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

`backend/.env`의 `MYSQL_PORT`만 다른 값(예: `3307`)으로 바꾸고 `docker compose up -d`를 다시 돌린다.
`build.gradle`이 `.env`를 읽어 `bootRun`/`test`에 넘겨주므로 다른 파일은 안 고쳐도 된다.

> IDE에서 `DdayApplication`의 main을 직접 실행하는 경우엔 `.env`를 읽지 않는다.
> 실행 구성의 환경변수에 `MYSQL_PORT`를 넣어준다.

### DB가 이상해졌을 때

`ddl-auto: update`는 컬럼 추가만 반영하고 **삭제·타입 변경은 반영하지 않는다.**
엔티티를 크게 바꾼 뒤 스키마가 꼬이면 DB를 새로 만든다:

```bash
cd backend && docker compose down -v && docker compose up -d
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

첫 도메인은 [backend/AGENTS.md §5·§6](./backend/AGENTS.md)의 코드 블록을 복사해서 시작하면 된다.

---

## 프론트엔드

```bash
cd frontend && npm install && npm run dev    # http://localhost:5173
```

백엔드도 같이 띄워야 API가 붙는다. 자세한 건 [frontend/README.md](./frontend/README.md).

**프론트는 백엔드 주소를 모른다.** 항상 `/api/...` 상대경로로 부르고,
로컬은 Vite 프록시가 · 배포는 Cloudflare Worker가 백엔드로 넘겨준다.
덕분에 환경 분기도 CORS 설정도 없다 — 이유는 [frontend/AGENTS.md §2](./frontend/AGENTS.md).
