# frontend

Vue 3 + TypeScript + Vite. Cloudflare Workers에 배포된다.

규칙은 [AGENTS.md](./AGENTS.md)에 있다. **API 호출 규칙은 꼭 읽고 시작할 것.**

## 시작하기

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
```

백엔드도 같이 띄워야 API가 붙는다 (다른 터미널):

```bash
cd backend && docker compose up -d && ./gradlew bootRun
```

`npm run dev`의 `/api`·`/health` 요청은 Vite가 `localhost:8080`으로 넘겨준다.

## 스크립트

| | |
|---|---|
| `npm run dev` | 개발 서버 (Vite) |
| `npm run build` | `dist/`로 빌드 |
| `npm run cf-dev` | **Worker까지 포함해서** 로컬 실행 — 배포와 같은 경로를 확인할 때 |
| `npm run deploy` | 빌드 + Cloudflare 배포 (보통은 push하면 자동으로 된다) |
| `npm run lint` / `format` | ESLint / Prettier |

`npm run dev`와 `npm run cf-dev`의 차이: `dev`는 Vite 프록시를 타고,
`cf-dev`는 실제 `worker/index.ts`를 타서 **배포된 것과 같은 경로**로 동작한다.
API 프록시가 이상하면 `cf-dev`로 확인한다.
