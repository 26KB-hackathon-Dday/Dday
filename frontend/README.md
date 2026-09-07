# frontend

Vue 3 + TypeScript + Vite.

코드 컨벤션은 [AGENTS.md](./AGENTS.md)에 있다. **API 호출 규칙(§2)은 읽고 시작할 것.**
커밋·PR 규칙은 [CONTRIBUTING.md](../CONTRIBUTING.md).

## 준비물

Node 22.18+ (또는 24.12+)

## 띄우기

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
```

**백엔드도 같이 띄워야 API가 붙는다** (다른 터미널):

```bash
cd backend && docker compose up -d && ./gradlew bootRun
```

`npm run dev`의 `/api` · `/health` 요청은 Vite가 `localhost:8080`으로 넘겨준다.

## 스크립트

| | |
|---|---|
| `npm run dev` | 개발 서버 (Vite) |
| `npm run build` | 타입 체크 + `dist/`로 빌드 |
| `npm run cf-dev` | **Worker까지 포함해서** 로컬 실행 |
| `npm run lint` | oxlint + ESLint (`--fix`) |
| `npm run format` | Prettier |

`dev`와 `cf-dev`의 차이: `dev`는 Vite 프록시를 타고, `cf-dev`는 실제 `worker/index.ts`를
탄다. API 프록시가 의심스러우면 `cf-dev`로 확인한다.

## 막혔을 때

| 증상 | 해결 |
|---|---|
| API가 전부 404 | 백엔드가 안 떠 있다 |
| 콘솔에 mixed content / CORS | 절대 URL로 불렀다. `/api/...` 상대경로를 쓴다 (AGENTS.md §2) |
| 새로고침하면 404 | `wrangler.jsonc`의 `not_found_handling` 확인 |
| `npm install`이 엔진 버전으로 실패 | Node 22.18+ 이 필요하다 |
