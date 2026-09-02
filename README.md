# Dday

26 KB IT's Your Life 해커톤

```
Dday/
├─ backend/     Spring Boot 3.5 + JPA  →  AWS EC2 (도커) + RDS
├─ frontend/    Vue 3 + TypeScript      →  Cloudflare Workers
└─ docs/        설계 메모 · 배포 가이드
```

브라우저 ──HTTPS──▶ Cloudflare Worker ──HTTP──▶ EC2 ──▶ RDS

frontend/** 수정 → Cloudflare Workers Builds

backend/**  수정 → GitHub Actions → GHCR → EC2

- **백엔드 컨벤션 → [backend/AGENTS.md](./backend/AGENTS.md)**
- **프론트엔드 컨벤션 → [frontend/AGENTS.md](./frontend/AGENTS.md)** 
- **배포 → [docs/deploy.md](./docs/deploy.md)**

---

## 시작하기

필요한 것: **JDK 17**, **Docker Desktop**

```bash
git clone https://github.com/26KB-hackathon-Dday/Dday.git
cd Dday/backend            

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


## 프론트엔드

```bash
cd frontend && npm install && npm run dev    # http://localhost:5173
```
