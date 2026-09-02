# Dday — 프론트엔드

**이 문서는 `frontend/`의 컨벤션 정본이다.** 같은 디렉터리의 `CLAUDE.md`가 이 파일을 가져다 쓴다.
백엔드 규칙은 `backend/AGENTS.md`에 따로 있다.

---

## 1. 스택

| 항목 | |
|---|---|
| Vue | 3.5 (Composition API + `<script setup>`) |
| 언어 | TypeScript |
| 빌드 | Vite |
| 라우팅 · 상태 | Vue Router · Pinia |
| 배포 | Cloudflare Workers (정적 자산 + API 프록시) |

---

## 2. API 호출 — **여기가 제일 중요하다**

### 항상 `/api/...` 상대경로를 쓴다. 절대 URL을 쓰지 않는다

```ts
await api.get('/api/members')            // ✅
await api.get('http://43.203.100.35/api/members')   // ❌
```

백엔드 주소를 코드에 박으면 안 되는 이유가 두 개다.

1. **브라우저가 차단한다.** 프론트는 HTTPS(Cloudflare)인데 백엔드는 HTTP(EC2)라
   mixed content로 막힌다
2. 로컬/배포에서 주소가 달라져 환경 분기가 코드에 스며든다

상대경로를 쓰면 **로컬은 Vite dev 프록시가, 배포는 `worker/index.ts`가** 백엔드로 넘겨준다.
브라우저 입장에선 프론트와 API가 같은 오리진이라 **CORS 설정도 필요 없다.**

백엔드 주소가 바뀌면 고칠 곳은 두 군데뿐이다 — `wrangler.jsonc`의 `BACKEND_ORIGIN`,
`vite.config.ts`의 `proxy.target`.

### `fetch`를 직접 부르지 말고 `src/api/client.ts`를 쓴다

```ts
import { api } from '@/api/client'

const members = await api.get<MemberListResponse[]>('/api/members')
await api.post('/api/members', { email, nickname })
```

백엔드는 성공·실패 모두 **같은 봉투**로 응답한다.

```json
{ "success": true,  "code": "MEMBERS_FOUND",   "message": "...", "data": [ ... ] }
{ "success": false, "code": "MEMBER_NOT_FOUND","message": "...", "data": null }
```

`api.*`가 봉투를 벗겨서 `data`만 돌려주고, `success: false`면 `ApiError`를 던진다.
**직접 `fetch`를 쓰면 이 처리를 매번 다시 짜게 된다.**

### 에러는 `message`를 그대로 띄우고, 분기는 `code`로 한다

```ts
try {
  await api.post('/api/members', form)
} catch (e) {
  if (e instanceof ApiError) {
    toast(e.message)              // 문구는 백엔드 ErrorCode enum이 정본이다
    if (e.code === 'EMAIL_DUPLICATED') { ... }   // 분기는 code로
    formErrors.value = e.fieldErrors             // 검증 실패 시 { 필드: 문구 }
  }
}
```

**문구를 프론트에서 새로 짓지 않는다.** 백엔드 `{도메인}ErrorCode`의 `message`가
화면 문구의 정본이고, 검증 실패 문구는 백엔드 검증 어노테이션의 `message`가 정본이다.
양쪽에서 따로 관리하면 반드시 어긋난다.

### API 목록은 Swagger에서 본다

- 로컬: http://localhost:8080/swagger-ui.html
- 배포: http://43.203.100.35/swagger-ui.html (백엔드 직접)

**단, Swagger는 정본이 아니다.** 검증 규칙(정규식·길이)과 에러 코드는 스키마에 안 실린다.
정확한 계약은 백엔드의 컨트롤러 · DTO · `ErrorCode` enum을 직접 본다.

---

## 3. 구조

```
frontend/
├─ src/
│  ├─ api/          client.ts(호출 래퍼) · types.ts(응답 봉투 타입)
│  ├─ views/        라우트 단위 화면
│  ├─ components/   재사용 컴포넌트
│  ├─ stores/       Pinia
│  └─ router/
├─ worker/          Cloudflare Worker (정적 서빙 + /api 프록시)
└─ wrangler.jsonc   Worker 설정
```

- 화면은 `views/`, 조각은 `components/`
- 도메인별 응답 타입은 `src/api/types.ts`가 아니라 각 도메인 가까이 두거나
  `src/api/{도메인}.ts`로 나눈다. `types.ts`는 **봉투 타입 전용**이다
- `@/`는 `src/` 별칭이다

---

## 4. 배포

배포 주소: **https://dday.26kb.workers.dev**

**`frontend/**`가 바뀐 채로 main에 push되면 Cloudflare Workers Builds가 자동 배포한다.**
백엔드 GitHub Actions는 경로 필터가 걸려 있어 이때 돌지 않는다 (그 반대도 같다).

배선을 직접 확인하려면 `npm run cf-dev`로 Worker까지 로컬에서 띄운다.
급하면 `npm run deploy`로 손으로 올릴 수도 있다(`wrangler login` 필요).

---

## 5. Git

**브랜치는 `main` 하나다.** 커밋은 `type: 한국어 설명` (`feat`, `fix`, `refactor`, `style`, `chore`).
push 전에 `git pull --rebase origin main`.

자세한 규칙과 이슈 템플릿은 `backend/AGENTS.md` §10에 있다 — 저장소 전체에 같이 적용된다.

---

## 6. Cloudflare 설정에서 알아야 할 것

- **`wrangler.jsonc`의 `name`은 대시보드의 Worker 이름과 같아야 한다.** 다르면 배포 단계에서
  실패한다. 지금은 양쪽 다 `dday`다 — 바꾸려면 두 곳을 같이 바꿔야 한다
- **Root directory는 `frontend`다.** 저장소 루트엔 `package.json`이 없어서,
  이게 `/`면 빌드가 `Could not read package.json`으로 죽는다 (실제로 첫 빌드가 이걸로 실패했다)
- **Build watch paths는 `frontend/*`** (저장소 루트 기준). 백엔드만 고친 커밋에
  프론트가 재빌드되지 않게 한다. GitHub Actions 쪽에 건 `paths: backend/**`의 반대쪽 짝이다
- **`BACKEND_ORIGIN`에 생 IP를 넣으면 안 된다.** 배포된 Worker가 IP로 fetch하면
  Cloudflare가 `1003 Direct IP access not allowed`로 막는다. EC2 퍼블릭 DNS 이름을 쓴다.
  **로컬 `wrangler dev`에서는 IP로도 동작해서 배포 전엔 안 드러난다**
