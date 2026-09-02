# frontend

아직 비어 있다. 스택이 정해지면 이 디렉터리 안에 프로젝트를 만든다.

```bash
# 예 — Vite + React
cd frontend
npm create vite@latest . -- --template react-ts
```

## 정할 때 같이 챙길 것

- **dev 서버 포트** — 백엔드 CORS 허용 목록에 있어야 한다.
  `backend/src/main/resources/application-local.yml`의 `app.cors.allowed-origins`.
  기본으로 `5173`(Vite)과 `3000`(CRA/Next)이 열려 있다. 다른 포트를 쓰면 여기에 추가한다
- **API 베이스 URL** — 로컬 `http://localhost:8080`, 배포는 EC2 주소.
  코드에 박지 말고 `.env`(`VITE_API_BASE_URL` 등)로 뺀다
- **응답 봉투** — 모든 API가 아래 모양으로 온다. axios 인터셉터에서 한 번에 벗기면 편하다

  ```json
  { "success": true, "code": "MEMBERS_FOUND", "message": "...", "data": { } }
  ```

  실패도 같은 모양이고 `data`는 `null`이다. 검증 실패일 때만 `errors` 배열이 추가된다.
  **화면에 띄울 문구는 `message`를 그대로 쓴다** — 백엔드 `ErrorCode` enum이 그 문구의 정본이다

- **API 목록** — 백엔드를 띄우고 http://localhost:8080/swagger-ui.html
