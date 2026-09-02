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

필요한 것: **JDK 17** · **Node 22.18+ 또는 24.12+** · **Docker**

터미널 두 개를 쓴다. **둘 다 떠 있어야 화면에 데이터가 나온다.**

```bash
# ① 백엔드
cd backend
cp .env.sample .env       # 최초 1회
docker compose up -d      # MySQL (빈 DB — 테이블과 시드는 앱이 뜰 때 들어간다)
./gradlew bootRun         # :8080
```

```bash
# ② 프론트엔드
cd frontend
npm install               # 최초 1회
npm run dev               # :5173
```

→ http://localhost:5173

| | |
|---|---|
| API 문서 | http://localhost:8080/swagger-ui.html |
| 백엔드만 확인 | http://localhost:8080/health/db |
| 3306이 이미 쓰이면 | `backend/.env`의 `MYSQL_PORT`만 바꾼다 |
| DB가 꼬였을 때 | `cd backend && docker compose down -v && docker compose up -d` |

---

- **백엔드 컨벤션 → [backend/AGENTS.md](./backend/AGENTS.md)**
- **프론트엔드 컨벤션 → [frontend/AGENTS.md](./frontend/AGENTS.md)**
- **배포 → [docs/deploy.md](./docs/deploy.md)**
