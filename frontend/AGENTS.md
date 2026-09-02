# Dday — 프론트엔드 컨벤션

`frontend/`의 규칙 정본. 같은 폴더의 `CLAUDE.md`가 이 파일을 가져다 쓴다.
백엔드 규칙은 `backend/AGENTS.md`에 있다.

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

## 2. API 호출

### 상대경로 + `api` 클라이언트

```ts
import { api } from '@/api/client'

const pockets = await api.get<Pocket[]>('/api/pockets')      // ✅
await fetch('http://43.203.100.35/api/pockets')              // ❌
```

주소를 코드에 박으면 배포 후 브라우저가 막는다. 프론트는 HTTPS(Cloudflare),
백엔드는 HTTP(EC2)라 mixed content가 된다.

상대경로는 로컬에서 Vite 프록시가, 배포에서 `worker/index.ts`가 백엔드로 넘긴다.
같은 오리진이므로 CORS 설정도 환경 분기도 없다.

백엔드 주소가 바뀌면 `wrangler.jsonc`의 `BACKEND_ORIGIN`과
`vite.config.ts`의 `proxy.target` 두 곳을 고친다.

### 응답 봉투

백엔드는 성공·실패를 같은 모양으로 준다.

```json
{ "success": true,  "code": "POCKETS_FOUND",    "message": "...", "data": [ ... ] }
{ "success": false, "code": "POCKET_NOT_FOUND", "message": "...", "data": null }
```

`api.*`가 봉투를 벗겨 `data`만 돌려주고, `success: false`면 `ApiError`를 던진다.
`fetch`를 직접 쓰면 이 처리를 매번 다시 짜게 된다.

### 에러 처리

```ts
try {
  await api.post('/api/pockets', form)
} catch (e) {
  if (e instanceof ApiError) {
    toast(e.message)                              // 화면 문구
    if (e.code === 'POCKET_NOT_FOUND') { ... }    // 분기는 code로
    formErrors.value = e.fieldErrors              // 검증 실패 시 { 필드: 문구 }
  }
}
```

`message`는 그대로 띄우고, 분기는 `code`로 한다.

### 표시용 값은 서버 것을 쓴다

에러 문구도, `HOUSING → 주거` 같은 라벨도 프론트에서 새로 만들지 않는다.
백엔드 `ErrorCode`의 `message`, 검증 어노테이션의 `message`, 응답의 `label`이
그 값들의 정본이다. 양쪽에서 관리하면 어긋난다.

### API 목록

로컬 http://localhost:8080/swagger-ui.html · 배포 http://43.203.100.35/swagger-ui.html

Swagger에는 검증 규칙(정규식·길이)과 에러 코드가 실리지 않는다.
정확한 계약은 백엔드의 컨트롤러 · DTO · `ErrorCode` enum을 본다.

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

- `types.ts`에는 봉투 타입만 둔다. 도메인 타입은 `api/{도메인}.ts`로
- `@/`는 `src/` 별칭
- 컴포넌트는 `<script setup lang="ts">`

---

## 4. 배포

https://dday.26kb.workers.dev

`frontend/**`가 바뀐 채로 main에 머지되면 Cloudflare Workers Builds가 배포한다.
백엔드 GitHub Actions엔 `paths: backend/**` 필터가 있어 이때 돌지 않는다(반대도 같다).

`npm run dev`는 Vite 프록시를 타므로 Worker 코드를 거치지 않는다.
API 프록시가 의심되면 `npm run cf-dev` — 실제 `worker/index.ts`로 배포와 같은 경로를 돈다.

Cloudflare 설정값.

| 항목 | 값 |
|---|---|
| Worker 이름 | `dday` — `wrangler.jsonc`의 `name`과 같아야 한다 |
| Root directory | `frontend` |
| Build watch paths | `frontend/*` (저장소 루트 기준) |
| `BACKEND_ORIGIN` | EC2 퍼블릭 DNS 이름 (IP는 안 된다 — 아래 참고) |

---

## 5. Git

`main`에 직접 push하지 않는다. 브랜치 → PR → Squash 머지.

```bash
git checkout main && git pull --rebase origin main
git checkout -b feat/pocket-card
# 구현 → npm run build 로 확인
git push -u origin feat/pocket-card
gh pr create --fill        # CI green이면 셀프 머지
```

커밋은 `type: 한국어 설명`. 전체 워크플로(이슈·라벨·충돌 해결)는
`backend/AGENTS.md` §11에 있다 — 저장소 전체에 적용된다.

---

## 막혔을 때

| 증상 | 원인 · 해결 |
|---|---|
| 배포하면 API가 전부 실패 (`error code: 1003`) | `BACKEND_ORIGIN`이 생 IP다. Worker는 IP로 fetch하지 못한다 — EC2 DNS 이름을 쓴다. 로컬 `wrangler dev`에선 IP로도 되므로 배포 전엔 드러나지 않는다 |
| 화면은 뜨는데 API만 죽음 | EC2 주소가 바뀌었다. `wrangler.jsonc`의 `BACKEND_ORIGIN` 확인 |
| 콘솔에 mixed content / CORS | 절대 URL로 불렀다. `/api/...` 상대경로로 바꾼다 |
| 배포가 `Could not read package.json` | Cloudflare의 Root directory가 `frontend`가 아니다 |
| 배포는 성공인데 반영이 안 됨 | 대시보드 Worker 이름과 `wrangler.jsonc`의 `name`이 다르다 |
| 새로고침하면 404 | `wrangler.jsonc`의 `not_found_handling: single-page-application` 확인 |
| 로컬에서 API가 404 | 백엔드가 안 떠 있다 |
| 백엔드만 고쳤는데 프론트가 재빌드됨 | Build watch paths가 비었다. `frontend/*` |
