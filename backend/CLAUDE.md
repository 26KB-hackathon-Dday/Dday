# CLAUDE.md — 백엔드

`backend/`의 코드 컨벤션과 작업 플로우 정본은 같은 디렉터리의 [AGENTS.md](./AGENTS.md)다.
아래 import로 전부 가져오므로, **규칙을 고칠 때는 `AGENTS.md`만 고친다.**
(Codex는 `AGENTS.md`를 직접 읽고 import를 따라가지 않으므로, 정본이 그쪽에 있어야 한다.)

@AGENTS.md

---

## Claude Code 전용

- 브랜치는 `main` 하나다. `develop`을 만들지 않는다.
- 커밋 메시지 끝에 트레일러를 붙인다:

  ```
  Co-Authored-By: Claude <noreply@anthropic.com>
  ```

- 로컬 MySQL 포트는 개발자마다 다를 수 있다 (`.env`의 `MYSQL_PORT`).
  접속이 안 되면 `docker compose ps`로 실제 포트를 먼저 확인한다.
- 스키마는 Flyway가 아니라 JPA `ddl-auto: update`가 만든다. 마이그레이션 파일을 찾지 말 것.
- **이 파일은 `backend/` 작업에만 적용된다.** 프론트엔드 작업은 `frontend/`의 규칙을 따른다.
