# Dday — 프론트엔드 컨벤션

`frontend/`의 규칙 정본. 같은 폴더의 `CLAUDE.md`가 이 파일을 가져다 쓴다.
백엔드 규칙은 `backend/AGENTS.md`에 따로 있다.

---

## 빠른 시작

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
```

백엔드도 같이 띄워야 API가 붙는다 (다른 터미널에서 `cd backend && ./gradlew bootRun`).

| 스크립트 | |
|---|---|
| `npm run dev` | 개발 서버 (Vite) |
| `npm run cf-dev` | **Worker까지 포함**해서 로컬 실행 — 배포와 같은 경로 |
| `npm run build` | `dist/`로 빌드 |
| `npm run deploy` | 손으로 배포 (`wrangler login` 필요) |
| `npm run lint` · `format` | ESLint · Prettier |

`dev`는 Vite 프록시를 타고, `cf-dev`는 실제 `worker/index.ts`를 탄다.
**API 프록시가 이상하면 `cf-dev`로 확인한다.**

---

## 1. 스택

| | |
|---|---|
| Vue | 3.5 (Composition API + `<script setup>`) |
| 언어 | TypeScript |
| 빌드 | Vite |
| 라우팅 · 상태 | Vue Router · Pinia |
| 배포 | Cloudflare Workers (정적 자산 + API 프록시) |

---

## 2. API 호출 — 여기가 제일 중요하다

### ① 항상 `/api/...` 상대경로. 절대 URL 금지

```ts
await api.get('/api/pockets')                        // ✅
await api.get('http://43.203.100.35/api/pockets')    // ❌ 배포하면 브라우저가 차단한다
```

프론트는 HTTPS(Cloudflare)인데 백엔드는 HTTP(EC2)라 절대 URL은 mixed content로 막힌다.
상대경로를 쓰면 **로컬은 Vite 프록시가, 배포는 `worker/index.ts`가** 백엔드로 넘겨준다.
같은 오리진이 되므로 **CORS 설정도, 환경 분기도 필요 없다.**

백엔드 주소가 바뀌면 고칠 곳은 두 군데다 —
`wrangler.jsonc`의 `BACKEND_ORIGIN`, `vite.config.ts`의 `proxy.target`.

### ② `fetch` 대신 `src/api/client.ts`

```ts
import { api } from '@/api/client'

const pockets = await api.get<Pocket[]>('/api/pockets')
await api.post('/api/pockets', { type, monthlyBudget })
```

백엔드는 성공·실패 모두 같은 봉투로 응답한다.

```json
{ "success": true,  "code": "POCKETS_FOUND",    "message": "...", "data": [ ... ] }
{ "success": false, "code": "POCKET_NOT_FOUND", "message": "...", "data": null }
```

`api.*`가 봉투를 벗겨 `data`만 돌려주고, `success: false`면 `ApiError`를 던진다.

### ③ 에러는 `message`를 그대로 띄우고, 분기는 `code`로

```ts
try {
  await api.post('/api/pockets', form)
} catch (e) {
  if (e instanceof ApiError) {
    toast(e.message)                              // 문구는 백엔드가 정본
    if (e.code === 'POCKET_NOT_FOUND') { ... }    // 분기는 code로
    formErrors.value = e.fieldErrors              // 검증 실패 시 { 필드: 문구 }
  }
}
```

**문구를 프론트에서 새로 짓지 않는다.** 백엔드 `{도메인}ErrorCode`의 `message`와
검증 어노테이션의 `message`가 정본이다. 양쪽에서 관리하면 반드시 어긋난다.

`label` 같은 표시용 값도 마찬가지다 — **`HOUSING → 주거` 매핑표를 프론트에 두지 않는다.**
서버가 내려주는 값을 쓴다.

### ④ API 목록은 Swagger, 계약은 코드

- 로컬 http://localhost:8080/swagger-ui.html · 배포 http://43.203.100.35/swagger-ui.html

**Swagger는 정본이 아니다.** 검증 규칙(정규식·길이)과 에러 코드는 스키마에 안 실린다.
정확한 계약은 백엔드의 컨트롤러 · DTO · `ErrorCode` enum을 직접 본다.

---

## 3. 구조 / 네이밍

```
frontend/
├─ src/
│  ├─ api/          client.ts(호출 래퍼) · types.ts(봉투 타입) · {도메인}.ts
│  ├─ views/        라우트 단위 화면
│  ├─ components/   재사용 조각
│  ├─ stores/       Pinia
│  └─ router/
├─ worker/          Cloudflare Worker (정적 서빙 + /api 프록시)
└─ wrangler.jsonc   Worker 설정
```

| 대상 | 규칙 | 예 |
|---|---|---|
| 화면 | `{이름}View.vue` — `views/` | `PocketView.vue` |
| 조각 | PascalCase — `components/` | `PocketCard.vue` |
| 도메인 API·타입 | `src/api/{도메인}.ts` | `api/pocket.ts` |
| Pinia 스토어 | `use{이름}Store` | `usePocketStore` |

- `types.ts`는 **봉투 타입 전용**이다. 도메인 타입은 `api/{도메인}.ts`에 둔다
- `@/`는 `src/` 별칭
- 컴포넌트는 전부 `<script setup lang="ts">`

---

## 4. 배포

배포 주소: **https://dday.26kb.workers.dev**

**`frontend/**`가 바뀐 채로 main에 push되면 Cloudflare Workers Builds가 자동 배포한다.**
백엔드 GitHub Actions엔 `paths: backend/**` 필터가 걸려 있어 이때 돌지 않는다(반대도 같다).

Cloudflare 쪽 설정 — 건드릴 일은 거의 없지만 알고는 있어야 한다.

| 항목 | 값 |
|---|---|
| Worker 이름 | `dday` — **`wrangler.jsonc`의 `name`과 반드시 같아야 한다** |
| Root directory | `frontend` |
| Build watch paths | `frontend/*` (저장소 루트 기준) |
| `BACKEND_ORIGIN` | EC2 퍼블릭 **DNS 이름** (IP 금지 — 부록 참고) |

---

## 5. Git

**브랜치는 `main` 하나.** 커밋은 `type: 한국어 설명`.
push 전에 `git pull --rebase origin main`.

전체 워크플로(이슈 → 브랜치 → PR)와 라벨은 **`backend/AGENTS.md` §11**에 있다 —
저장소 전체에 같이 적용된다.

---

## 부록 — 자주 밟는 함정

| 증상 | 원인 · 해결 |
|---|---|
| 배포하면 API가 전부 실패 (`error code: 1003`) | `BACKEND_ORIGIN`이 생 IP다. Worker는 IP로 fetch 못 한다 — EC2 DNS 이름을 쓴다. **로컬 `wrangler dev`에선 IP로도 되니 배포 전엔 안 드러난다** |
| 화면은 뜨는데 API만 죽음 | EC2 주소가 바뀌었다. `wrangler.jsonc`의 `BACKEND_ORIGIN` 확인 |
| 브라우저 콘솔에 mixed content / CORS | 절대 URL로 불렀다. `/api/...` 상대경로로 바꾼다 |
| 배포가 `Could not read package.json` | Cloudflare의 Root directory가 `frontend`가 아니다 |
| 배포는 성공인데 반영이 안 됨 | 대시보드 Worker 이름과 `wrangler.jsonc`의 `name`이 다르다 |
| 새로고침하면 404 | `wrangler.jsonc`의 `not_found_handling: single-page-application` 확인 |
| 로컬에서 API가 404 | 백엔드가 안 떠 있다. `cd backend && ./gradlew bootRun` |
| 백엔드만 고쳤는데 프론트가 재빌드됨 | Build watch paths가 비었다. `frontend/*` |
