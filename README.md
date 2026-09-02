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

---

## 로컬 실행

JDK 17 · Node 22.18+ · Docker

```bash
cd backend
docker compose up -d
./gradlew bootRun          # :8080
```

```bash
cd frontend
npm install
npm run dev                # :5173
```

http://localhost:5173

- API 문서 — http://localhost:8080/swagger-ui.html
- 3306이 이미 쓰이면 — `backend/.env`의 `MYSQL_PORT`
- DB 초기화 — `cd backend && docker compose down -v && docker compose up -d`

---

- **백엔드 컨벤션 → [backend/AGENTS.md](./backend/AGENTS.md)**
- **프론트엔드 컨벤션 → [frontend/AGENTS.md](./frontend/AGENTS.md)**
- **배포 → [docs/deploy.md](./docs/deploy.md)**
