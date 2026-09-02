# CLAUDE.md — 프론트엔드

`frontend/`의 컨벤션 정본은 같은 디렉터리의 [AGENTS.md](./AGENTS.md)다.

@AGENTS.md

---

## Claude Code 전용

- **이 파일은 `frontend/` 작업에만 적용된다.** 백엔드는 `backend/`의 규칙을 따른다.
- 커밋 메시지 끝에 트레일러를 붙인다:

  ```
  Co-Authored-By: Claude <noreply@anthropic.com>
  ```

- **API를 부를 때 절대 URL을 쓰지 않는다.** 항상 `/api/...` 상대경로 + `src/api/client.ts`.
  이유는 AGENTS.md §2에 있다 — 절대 URL은 배포하면 브라우저가 차단한다.
- 백엔드 API 계약을 확인해야 하면 `backend/src/main/java/com/dday/`의
  컨트롤러 · DTO · `ErrorCode` enum을 직접 읽는다. Swagger는 사본이다.
