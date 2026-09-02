# 배포

**main에 push하면 자동으로 배포된다.** 아래는 이미 만들어져 돌고 있는 것이고,
§1부터는 그 배선을 처음부터 다시 만드는 방법이다.

## 지금 떠 있는 것

| | |
|---|---|
| **API** | http://43.203.100.35 · [/health/db](http://43.203.100.35/health/db) · [Swagger](http://43.203.100.35/swagger-ui.html) |
| EC2 | `dday-app` / `i-008d5a4c6e6272147` / t3.small / `ap-northeast-2a` |
| RDS | `dday-db` / MySQL 8.4.11 / db.t4g.micro / 파라미터 그룹 `dday-mysql84` (KST) |
| 보안 그룹 | `dday-ec2-sg` `sg-05e44a69d6daac812` · `dday-rds-sg` `sg-02c97022f7b0cc81d` |
| 계정 · 리전 | `715975222399` · `ap-northeast-2` |

- **SSH 키는 `~/.ssh/dday-key.pem`** (만든 사람 로컬에만 있다). 팀원이 서버에 붙어야 하면
  이 파일을 따로 전달한다 — 저장소에 넣지 않는다
- **RDS 엔드포인트와 마스터 암호는 서버 `~/dday/.env`에 있다.** 이 저장소는 public이라
  적지 않는다. 필요하면 아래로 꺼낸다

  ```bash
  aws rds describe-db-instances --db-instance-identifier dday-db \
    --query 'DBInstances[0].Endpoint.Address' --output text
  ssh -i ~/.ssh/dday-key.pem ec2-user@43.203.100.35 'cat ~/dday/.env'
  ```

- 노트북에서 GUI 클라이언트로 RDS에 붙으려면 **`dday-rds-sg`의 3306에 본인 IP `/32`를
  추가**해야 한다 (기본은 만든 사람 IP만 열려 있다)

---

이 문서는 그 배선이 어떻게 생겼는지와, AWS를 처음 한 번 세팅하는 방법이다.

```
main push
  └─ build    컴파일 + 테스트 (MySQL service container)
     └─ image    도커 이미지 빌드 → GHCR push (ghcr.io/26kb-hackathon-dday/dday-backend)
        └─ deploy   EC2에 SSH → docker compose pull && up -d → /health/db 확인
```

`deploy` 잡은 **저장소 Variable `DEPLOY_ENABLED=true`일 때만** 돈다.
AWS를 아직 안 만들었으면 통째로 건너뛰므로 main push가 빨갛게 뜨지 않는다.

---

## 구성

```
    인터넷
      │  :80
      ▼
 ┌──────────────┐   :3306    ┌──────────────┐
 │ EC2 t3.small │ ─────────▶ │ RDS MySQL 8.4│
 │  dday-backend│  (SG 체인) │   dday-db    │
 │  (컨테이너)   │            └──────────────┘
 └──────────────┘
```

- **앱은 EC2 1대**에 도커 컨테이너로. 로드밸런서도 EB도 ECS도 쓰지 않는다
- **DB는 RDS.** 팀이 이미 다뤄본 구성이라 3일 안에 처음 보는 에러를 만날 확률이 낮다

> **Elastic Beanstalk은 쓰지 않는다.** `UpdateEnvironment`가 내부적으로 CloudFormation을
> 돌려서 여러 서비스 권한을 연쇄로 요구한다. 최소 권한에서 출발해 거부될 때마다 하나씩
> 추가하는 방식은 수렴 지점이 없고, 거기에 GitHub OIDC 신뢰 정책의 `sub` claim 형식 문제까지
> 겹치면 반나절이 날아간다 (같은 팀의 이전 프로젝트에서 4번 실패한 기록이 있다).
> 지금 구성에서 배포에 필요한 비밀값은 **SSH 키 하나**뿐이고 IAM 역할이 없다.

**로컬 개발은 RDS에 붙지 않는다.** 로컬은 `backend/docker-compose.yml`의 MySQL 컨테이너를 쓴다.
5명이 같은 DB를 밟으면 서로의 데이터를 지운다.

---

## 1. RDS 만들기 (최초 1회)

콘솔에서 만든다면 이 값만 맞추면 된다.

| 항목 | 값 |
|---|---|
| 엔진 | MySQL **8.4.x** (로컬 도커와 같은 메이저를 쓴다) |
| 템플릿 | 개발/테스트 |
| 인스턴스 | `db.t4g.micro` |
| 스토리지 | gp3 20 GiB, 자동 확장 끔 |
| 다중 AZ | **아니요** |
| 식별자 | `dday-db` |
| 마스터 사용자 | `admin` |
| 마스터 암호 | 길고 랜덤하게. **여기서 한 번 보고 못 본다 — 바로 서버 `.env`에 넣는다** |
| 퍼블릭 액세스 | **예** (각자 노트북에서 GUI 클라이언트로 붙어야 하니까) |
| 초기 DB 이름 | `dday` ← **"추가 구성"을 펼쳐야 나온다. 빼먹으면 DB가 안 만들어진다** |
| 파라미터 그룹 | `dday-mysql84` (아래에서 만든다) |
| 백업 보존 | 1일 |

> ⚠️ **인스턴스 크기를 키우려면 계정 플랜을 먼저 확인한다.** 신형 무료 플랜 계정은
> `db.t4g.micro`보다 큰 크기를 `FreeTierRestrictionError: This instance size isn't available
> with free plan accounts`로 거부한다. **크레딧 잔액과 무관하고, 유료 플랜 전환만이 해제 방법이다.**
> 해커톤 규모에서 `db.t4g.micro`면 충분하니 굳이 건드리지 않는 게 낫다.

### 파라미터 그룹 — 빼먹으면 시간이 9시간 틀어진다

RDS 기본 파라미터 그룹은 수정할 수 없어서 새로 만들어야 한다.
**기본 `time_zone`은 UTC라서, 안 바꾸면 `NOW()`가 한국 시각보다 9시간 이르게 찍힌다.**

```bash
aws rds create-db-parameter-group \
  --db-parameter-group-name dday-mysql84 \
  --db-parameter-group-family mysql8.4 \
  --description "Dday MySQL 8.4 (KST)"

aws rds modify-db-parameter-group \
  --db-parameter-group-name dday-mysql84 \
  --parameters "ParameterName=time_zone,ParameterValue=Asia/Seoul,ApplyMethod=immediate"
```

`time_zone`은 동적 파라미터라 재부팅 없이 적용된다. 만든 뒤 확인:

```sql
SELECT @@global.time_zone, NOW();   -- Asia/Seoul 과 한국 시각이 나와야 한다
```

> 로컬 도커 MySQL은 이름(`Asia/Seoul`) 대신 오프셋(`+09:00`)을 쓴다.
> 공식 이미지가 `mysql.time_zone_*` 테이블을 비워두기 때문인데, **RDS는 그 테이블이 채워져 있어
> 이름을 그대로 쓸 수 있다.** 두 파일이 다른 이유가 이거다.

### 보안 그룹

`dday-rds-sg`를 만들고 인바운드에 아래를 넣는다.

| 포트 | 소스 | 용도 |
|---|---|---|
| 3306 | **EC2 보안 그룹(`dday-ec2-sg`)** | 앱 → DB |
| 3306 | 각자 노트북 IP `/32` | GUI 클라이언트로 직접 조회 |

> ⚠️ **3306을 `0.0.0.0/0`으로 열지 않는다.** 봇이 몇 시간 안에 스캔해서 마스터 계정에
> 사전 공격을 건다. 카페 와이파이를 옮겨서 IP가 바뀌면 그때 규칙을 하나 더 추가한다
> (귀찮다고 전체 개방하지 말 것 — 계정이 털리면 크레딧이 채굴에 쓰인다).

---

## 2. EC2 만들기 (최초 1회)

| 항목 | 값 |
|---|---|
| AMI | Amazon Linux 2023 |
| 인스턴스 타입 | **t3.small** (2 GiB) |
| 스토리지 | gp3 20 GiB |
| 리전 | `ap-northeast-2` (서울) — **RDS와 같은 VPC** |
| 키페어 | 새로 만들고 `.pem`을 받아둔다 — GitHub Secret으로 쓴다 |
| 퍼블릭 IP | 자동 할당 켬 |

DB가 이 박스에 없으니 t3.small로 충분하다. 앱 컨테이너 하나만 돈다.

**보안 그룹 `dday-ec2-sg`**

| 포트 | 소스 | 용도 |
|---|---|---|
| 22 | **0.0.0.0/0** | SSH — GitHub Actions 러너가 배포하러 들어온다 |
| 80 | 0.0.0.0/0 | API |

이 보안 그룹 ID가 §1의 RDS 인바운드 소스로 들어간다.

> ⚠️ **22를 전체 개방한 이유가 있다.** GitHub Actions 러너는 IP가 매번 바뀌고,
> GitHub이 공개하는 러너 대역은 4천 개가 넘어서 보안 그룹(규칙 60개 한도)에 못 넣는다.
> 내 IP만 열어두면 배포가 `dial tcp :22: i/o timeout`으로 실패한다 —
> 실제로 첫 배포가 이걸로 한 번 죽었다.
>
> **그래도 괜찮은 이유**: AL2023 기본값이 키 인증 전용이다(`sshd -T`로 확인:
> `passwordauthentication no`, `kbdinteractiveauthentication no`). 봇이 두드려도
> 대입할 암호가 없다. 3일 뒤 terminate할 인스턴스이기도 하다.
>
> 그래도 닫아두고 싶으면 대안은 **배포 잡이 시작할 때 러너 IP로 22를 열고 끝나면
> 닫는 것**이다. `ec2:AuthorizeSecurityGroupIngress`/`Revoke...` 두 권한만 가진 IAM
> 사용자와 액세스 키 2개가 필요하고, **잡이 중간에 죽으면 규칙이 열린 채 남는**
> 실패 모드가 새로 생긴다. 3일 동안 그걸 지켜볼 사람이 없어서 채택하지 않았다.

---

## 3. 서버 초기 세팅 (최초 1회)

```bash
ssh -i ~/받은키.pem ec2-user@{퍼블릭IP}
```

```bash
# 도커
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user

# docker compose 플러그인 (AL2023 기본 저장소에 없어서 직접 받는다)
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 그룹 변경을 반영하려면 다시 접속해야 한다
exit
```

다시 접속해서 배포 디렉터리와 `.env`를 만든다.

```bash
mkdir -p ~/dday && cd ~/dday

cat > .env <<'EOF'
DB_HOST=dday-db.xxxxxxxx.ap-northeast-2.rds.amazonaws.com
DB_NAME=dday
DB_USERNAME=admin
DB_PASSWORD={RDS 마스터 암호}
CORS_ALLOWED_ORIGINS=
EOF
chmod 600 .env
```

> **`DB_HOST`에는 호스트만 넣는다.** `jdbc:` 접두사도, `:3306`도 붙이지 않는다.
> JDBC URL은 `backend/docker-compose.prod.yml`이 조립한다 — 그래야 `serverTimezone` 같은
> 파라미터를 누가 붙여넣다 빠뜨리는 일이 없다.
>
> **이 `.env`는 서버에만 있고 저장소에는 없다.** Actions는 `backend/docker-compose.prod.yml`을
> 서버의 `~/dday/docker-compose.prod.yml`로 덮어쓸 뿐 `.env`는 건드리지 않으므로,
> 한 번 만들어두면 배포할 때마다 유지된다.

연결이 되는지 먼저 확인해두면 뒤가 편하다.

```bash
sudo dnf install -y mariadb105    # mysql 클라이언트
mysql -h $DB_HOST -u admin -p -e "SELECT @@global.time_zone, NOW();"
```

여기서 막히면 **보안 그룹 체인(§1)**이 문제다. 앱을 올려도 똑같이 막힌다.

---

## 4. GitHub 설정

**Settings → Secrets and variables → Actions**

| 종류 | 이름 | 값 |
|---|---|---|
| Secret | `EC2_HOST` | EC2 퍼블릭 IP |
| Secret | `EC2_USER` | `ec2-user` |
| Secret | `EC2_SSH_KEY` | `.pem` 파일 **내용 전체** (`-----BEGIN ...` 줄 포함) |
| Variable | `DEPLOY_ENABLED` | `true` |

`DEPLOY_ENABLED`는 **Variables 탭**이다 (Secrets 아님). 이 값이 `true`가 아니면
`deploy` 잡을 건너뛴다.

> **DB 접속정보는 GitHub에 넣지 않는다.** 서버 `.env`에만 있다.
> Actions는 이미지를 만들고 재시작을 시킬 뿐, DB에 붙지 않는다.

### GHCR 이미지는 private로 둔다

저장소가 public이어도 **GHCR 패키지는 기본이 private다.** 여기서는 그걸 그대로 둔다.

`deploy` 잡이 pull 직전에 **그 잡에서만 유효한 `GITHUB_TOKEN`으로 서버에서 `docker login`**
을 하기 때문이다. 패키지를 공개할 필요도, `read:packages` PAT를 만들어 서버에 둘 필요도 없다.
따로 할 일은 없고, 이 문단은 "왜 public으로 안 바꾸나"에 대한 답이다.

---

## 5. 첫 배포

Actions 탭 → CI → **Run workflow** → branch `main`.
코드를 안 바꿔도 배포까지 그대로 이어진다. 배선만 먼저 확인할 때 쓴다.

성공하면 워크플로 마지막 단계가 `/health/db` 응답을 찍는다:

```json
{"success":true,"code":"DB_HEALTHY","message":"데이터베이스 연결이 정상입니다.","data":1}
```

브라우저로도 확인:
- `http://{퍼블릭IP}/health/db`
- `http://{퍼블릭IP}/swagger-ui.html`

**스키마는 앱이 만든다.** `ddl-auto: update`라서 엔티티가 있으면 첫 기동 때 테이블이 선다.
지금은 엔티티가 없으니 빈 DB로 뜨는 게 정상이다.

---

## 6. 안 될 때

```bash
ssh -i ~/받은키.pem ec2-user@{퍼블릭IP}
cd ~/dday
docker compose -f docker-compose.prod.yml logs -f backend
```

| 증상 | 원인 |
|---|---|
| `pull` 이 `denied` | GHCR 패키지가 아직 private다 (§4) |
| 로그에 `Communications link failure` / 기동이 60초쯤 걸리다 죽음 | **보안 그룹 체인.** RDS 인바운드에 EC2 SG가 없다 |
| 로그에 `Access denied for user` | `.env`의 `DB_PASSWORD`가 틀렸다 |
| 로그에 `Unknown database 'dday'` | RDS 만들 때 "추가 구성"의 초기 DB 이름을 빼먹었다. 직접 `CREATE DATABASE dday;` |
| 시각이 9시간 이르다 | 파라미터 그룹의 `time_zone`이 적용 안 됐다 (§1) |
| `/health`는 되는데 `/health/db`가 500 | 위 넷 중 하나. 로그를 본다 |
| 응답 자체가 없음 | EC2 보안 그룹에 80이 안 열렸거나 컨테이너가 안 떴다 |

DB를 통째로 비우고 싶으면 (**데이터가 전부 사라진다. 시연 직전엔 하지 않는다**):

```sql
DROP DATABASE dday; CREATE DATABASE dday;
```

앱을 재시작하면 `ddl-auto: update`가 테이블을 다시 만든다.

---

## 7. 롤백

이미지에 커밋 sha 태그가 같이 붙어 있어서 이전 커밋으로 되돌릴 수 있다.
가장 안 헷갈리는 방법은 **되돌리고 싶은 커밋을 revert해서 main에 push**하는 것이다 —
파이프라인이 알아서 다시 배포한다.

서버에서 직접 되돌리려면:

```bash
cd ~/dday
sed -i 's|:latest|:{되돌릴 커밋 sha}|' docker-compose.prod.yml
docker compose -f docker-compose.prod.yml up -d
```

> 다음 배포 때 Actions가 이 파일을 다시 덮어쓰므로, 손으로 고친 태그는 오래 유지되지 않는다.

**DB는 롤백되지 않는다.** `ddl-auto: update`는 컬럼을 지우지 않으니 앱만 되돌리면
대개 그대로 동작하지만, 스키마를 크게 바꾼 뒤라면 RDS 스냅샷에서 복원해야 한다
(백업 보존 1일이면 그 안의 시점으로만 가능하다).

---

## 8. 비용

`ap-northeast-2` 기준 개략치. 확정 전에 AWS 요금 페이지로 확인할 것.

| 항목 | 시간당 | 3일 |
|---|---|---|
| EC2 t3.small | ~$0.026 | $1.87 |
| RDS db.t4g.micro | ~$0.020 | $1.44 |
| 퍼블릭 IPv4 **2개** (EC2 + 퍼블릭 RDS) | $0.010 | $0.72 |
| 스토리지 (EBS 20 + RDS 20) | ~$0.006 | $0.43 |
| **합계** | **~$0.062** | **~$4.5** |

> 퍼블릭 IPv4는 2024-02-01부터 **붙어 있기만 해도** $0.005/hr다.
> "쓰고 있으면 무료"는 폐지된 규칙이라 계산에서 빼먹기 쉽다.
> RDS를 퍼블릭으로 열었기 때문에 여기서 2개를 쓴다.

**해커톤이 끝나면 정리한다.**

```bash
aws ec2 terminate-instances --instance-ids i-008d5a4c6e6272147
aws ec2 release-address --allocation-id eipalloc-0663f5d09ebea546a   # ← 잊기 쉽다
aws rds delete-db-instance --db-instance-identifier dday-db \
  --final-db-snapshot-identifier dday-db-final
```

- stop만 하면 스토리지 요금이 계속 나가고, **RDS는 stop해도 7일 뒤 자동으로 다시 켜진다**
- ⚠️ **EIP는 인스턴스를 지워도 남고, 안 붙어 있으면 오히려 계속 과금된다**($0.005/hr).
  `release-address`를 빼먹는 게 잊혀진 청구서의 단골이다

---

## 9. 프론트엔드(Cloudflare)와의 접점

프론트는 `https://dday.26kb.workers.dev`에서 뜨고, `/api/*`와 `/health`를
Cloudflare Worker가 이 EC2로 넘긴다. 브라우저는 백엔드를 직접 부르지 않는다.

Worker가 바라보는 주소는 `frontend/wrangler.jsonc`의 `BACKEND_ORIGIN`에 있다.

> ⚠️ **거기에 생 IP를 넣으면 안 된다.** 배포된 Worker가 IP로 fetch하면 Cloudflare가
> `error code: 1003 Direct IP access not allowed`로 403을 준다. **EC2 퍼블릭 DNS 이름**
> (`ec2-43-203-100-35.ap-northeast-2.compute.amazonaws.com`)을 쓴다.
> 로컬 `wrangler dev`에서는 IP로도 잘 되기 때문에 배포하기 전엔 드러나지 않는다.

**이 호스트명에는 IP가 박혀 있어서, 주소가 바뀌면 프론트의 API가 통째로 죽는다.**
화면은 멀쩡히 뜨는데 API만 전부 실패하는 형태라 원인이 잘 안 드러난다.
그래서 **Elastic IP `eipalloc-0663f5d09ebea546a`를 붙여 고정해뒀다** —
실행 중인 인스턴스에 붙어 있는 동안의 요금은 자동 할당 IP와 같다(둘 다 $0.005/hr).

주소가 바뀌는 일이 생기면(EIP를 떼거나 인스턴스를 새로 만들면) **세 곳을 같이 고쳐야 한다.**

| 고칠 곳 | 값 |
|---|---|
| `frontend/wrangler.jsonc`의 `BACKEND_ORIGIN` | EC2 퍼블릭 **DNS 이름** |
| GitHub Secret `EC2_HOST` | EC2 **IP** |
| 이 문서와 `README.md` | 양쪽 |

앞의 둘 중 하나만 고치면 조용히 반쪽만 동작한다 — 프론트는 되는데 배포가 실패하거나,
그 반대가 된다.

---

## 10. 이 구성이 안 하는 것

- **HTTPS가 없다.** 도메인이 없으면 인증서를 받을 수 없다

  > ⚠️ **프론트를 Vercel/Netlify 같은 HTTPS 호스팅에 올리면 이 백엔드를 호출하지 못한다.**
  > HTTPS 페이지에서 HTTP API를 부르는 건 브라우저가 mixed content로 차단한다.
  > 셋 중 하나를 골라야 한다:
  > ① 프론트도 같은 EC2에 http로 올린다 (가장 간단, 데모엔 충분)
  > ② 도메인을 하나 사서 Let's Encrypt로 인증서를 붙인다 (1~2시간)
  > ③ 프론트를 로컬에서 띄워 시연한다
  >
  > **프론트 배포 위치를 정할 때 이걸 먼저 확인한다.** 마지막 날에 발견하면 늦는다.

- **무중단 배포가 아니다.** `up -d`가 컨테이너를 갈아끼우는 몇 초 동안 502가 난다.
  시연 중에는 main에 push하지 않는다
- **EC2가 죽으면 자동 복구되지 않는다.** ASG도 헬스체크 기반 교체도 없다.
  대신 **DB는 EC2와 분리돼 있어서, 인스턴스를 새로 만들어도 데이터는 살아 있다**
- **모니터링이 없다.** 로그는 `docker compose logs`로 직접 본다
