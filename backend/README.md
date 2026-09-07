# backend

Spring Boot 3.5 + JPA + MySQL 8.4.

코드 컨벤션은 [AGENTS.md](./AGENTS.md)에, 커밋·PR 규칙은
[CONTRIBUTING.md](../CONTRIBUTING.md)에 있다.

## 준비물

JDK 17 · Docker

## 띄우기

```bash
cd backend
cp .env.sample .env       # 처음 한 번만
docker compose up -d      # MySQL
./gradlew bootRun         # :8080
```

MySQL만 컨테이너로 띄우고 **앱은 직접 돌린다.** 앱까지 컨테이너에 넣으면
코드 한 줄 고칠 때마다 이미지를 다시 구워야 한다.

- API 문서 — http://localhost:8080/swagger-ui.html
- 헬스체크 — http://localhost:8080/health/db (`/health`는 DB 단절을 못 잡는다)

프로파일 기본값이 `local`이라 따로 지정할 게 없다. `application-local.yml`의 값이
`docker-compose.yml` · `.env.sample`과 짝이 맞다.

## 테스트

```bash
./gradlew build           # 컴파일 + 테스트
./gradlew test
```

MySQL 컨테이너가 떠 있어야 한다.

## 막혔을 때

| 증상 | 해결 |
|---|---|
| `Port 3306 is already allocated` | `.env`의 `MYSQL_PORT`를 바꾸고, `application-local.yml`의 URL 포트도 같이 고친다 |
| 앱이 DB에 못 붙는다 | `docker compose ps`로 healthy인지 확인한다. 기동에 몇 초 걸린다 |
| 스키마가 꼬였다 | `docker compose down -v && docker compose up -d` — 데이터까지 날리고 새로 만든다 |
| `data.sql`에서 실패하며 앱이 안 뜬다 | 의도된 동작이다. 시드가 멱등한지 본다 (AGENTS.md §9) |
| 저장된 시각이 9시간 어긋난다 | 컨테이너를 만든 뒤 타임존 설정을 바꿨다. `down -v` 후 다시 올린다 |
