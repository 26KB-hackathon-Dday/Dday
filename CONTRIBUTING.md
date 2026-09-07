# 기여 가이드

저장소 전체에 적용되는 Git 워크플로우다.
코드 컨벤션은 각 디렉터리에 따로 있다 —
[`backend/AGENTS.md`](./backend/AGENTS.md) · [`frontend/AGENTS.md`](./frontend/AGENTS.md).

**`main`에 직접 push하지 않는다.** 룰셋이 서버에서 막는다(관리자도 예외 없음).
브랜치 → PR → **Squash 머지**, 승인은 필요 없다.

---

## 흐름

**① 이슈 등록** — 여럿이 알아야 할 덩어리에만. 작은 수정은 ②부터 시작한다.
제목은 `[TYPE] 한국어 설명`, **접두사와 라벨을 1:1로 맞춘다.**

| 접두사 | 라벨 | 템플릿 |
|---|---|---|
| `[FEAT]` | `✨ 기능` | `feature_request` |
| `[BUG]` | `🐛 버그` | `bug_report` |
| `[TASK]` | `🛠️ 작업` | `task` |
| `[DOCS]` | `📝 문서` | `docs` |

영역 라벨(`🌐 API` · `🗄️ DB` · `🎨 프론트` · `🧰 인프라` · `🧹 리팩터링`)을 추가로 붙이면
나중에 훑기 좋다. 막고 있는 이슈면 `🔥 긴급`.

**② 브랜치** — `{type}/{영어-소문자-하이픈}`

```bash
git checkout main && git pull --rebase origin main
git checkout -b feat/pocket-budget
```

**③ 구현 → 빌드로 확인** — 백엔드 `./gradlew build`, 프론트 `npm run build`

**④ 커밋** — `type: 한국어 설명`

```
feat: 포켓별 월 배분액을 계산한다
```

`feat` `fix` `docs` `chore` `refactor` `test` `style` `ci`.
작업과 무관한 파일을 같이 add하지 않는다.

**⑤ push** — `git push -u origin feat/pocket-budget`

**⑥ PR** — 제목 `[#이슈번호] type: 작업 내용`(이슈가 없으면 `type: 작업 내용`),
본문에 `closes #N`. 템플릿이 자동으로 붙는다.

```bash
gh pr create --fill        # 또는 push 후 뜨는 링크로
```

**⑦ 머지** — **Squash and merge**. 브랜치의 잡다한 커밋이 `main`에 한 줄로 들어간다.
머지 후 브랜치는 지우고, `main`으로 돌아와 `git pull --rebase origin main`.

> **리뷰 승인을 기다리지 않는다.** CI가 green이면 셀프 머지해도 된다.
> 봐줬으면 하는 게 있으면 PR 링크를 팀에 직접 던진다.

---

## 충돌이 났을 때

`main`이 앞서 나가 PR에 충돌이 뜨면 **브랜치에서 rebase**한다.

```bash
git checkout feat/pocket-budget
git fetch origin && git rebase origin/main
# 충돌 해결 후
git add . && git rebase --continue
git push --force-with-lease        # --force 말고 이걸 쓴다
```

`--force-with-lease`는 그 사이 남이 같은 브랜치에 push했으면 거부한다.
그냥 `--force`는 남의 커밋을 말없이 지운다.
