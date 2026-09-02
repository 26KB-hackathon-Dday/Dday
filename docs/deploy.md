# 배포

**main에 push하면 자동으로 배포된다.** 이 문서는 그 배선이 어떻게 생겼는지와,
EC2를 처음 한 번 세팅하는 방법이다.

```
main push
  └─ build    컴파일 + 테스트 (MySQL service container)
     └─ image    도커 이미지 빌드 → GHCR push (ghcr.io/26kb-hackathon-dday/dday-backend)
        └─ deploy   EC2에 SSH → docker compose pull && up -d → /health/db 확인
```

`deploy` 잡은 **저장소 Variable `DEPLOY_ENABLED=true`일 때만** 돈다.
EC2를 아직 안 만들었으면 통째로 건너뛰므로 main push가 빨갛게 뜨지 않는다.

---

## 왜 EC2 1대인가

Elastic Beanstalk / ECS / App Runner를 쓰지 않는다. **셋 다 3일짜리엔 손해다.**

- **EB는 IAM 지옥이 있다.** `UpdateEnvironment`가 내부적으로 CloudFormation을 돌려서
  여러 서비스 권한을 연쇄로 요구한다. 최소 권한에서 출발해 거부될 때마다 하나씩 추가하는
  방식은 수렴 지점이 없다 (같은 팀의 이전 프로젝트에서 4번 실패하고 관리형 정책으로 갈아탄 기록이 있다).
  거기에 GitHub OIDC 신뢰 정책의 `sub` claim 형식 문제까지 겹치면 반나절이 날아간다
- **ECS는 태스크 정의 · 서비스 · ALB · ECR까지 만들 게 너무 많다**
- **App Runner + RDS는 최소 구성만으로도 월 $25 이상**이다

EC2 1대에 docker compose로 MySQL과 백엔드를 같이 올리면 **RDS도, 로드밸런서도, IAM 역할도
필요 없다.** 필요한 비밀값은 SSH 키 하나뿐이다.

**대가로 포기하는 것**: 무중단 배포(재시작 중 몇 초 끊긴다), DB 자동 백업,
인스턴스가 죽으면 자동 복구. 3일 데모에는 셋 다 필요 없다.

### RDS를 안 쓰는 이유는 돈이 아니다

`db.t4g.micro`는 서울 리전에서 시간당 약 $0.02다. **3일이면 $1.5 남짓**이라
비용 차이는 사실상 없다. 안 쓰는 이유는 두 가지다.

- **세팅 시간** — 서브넷 그룹, 보안 그룹(EC2 SG → 3306), `time_zone=Asia/Seoul`을 위한
  파라미터 그룹, 그리고 `available`이 될 때까지의 대기. 잘 풀려도 20~30분이고,
  처음 만들면 대개 한 번은 막힌다
- **실패 지점이 늘어난다** — 첫날 배포가 안 될 때 "앱이 문제인지, 보안 그룹인지,
  파라미터 그룹인지"를 가리는 데 시간이 든다. compose MySQL은 같은 도커 네트워크 안이라
  이 층이 통째로 없다

**대신 감수하는 것**: 자동 백업이 없고, 인스턴스를 terminate하면 데이터도 같이 사라진다.
그리고 MySQL과 JVM이 한 박스의 메모리를 나눠 쓴다(§2 참고).

### 그래도 RDS로 가야 하는 경우

아래 중 하나라도 해당되면 RDS가 맞다.

- **시연용 데이터를 손으로 오래 쌓아야 한다** — 날리면 복구가 안 되는 상황
- **인스턴스를 껐다 켜거나 타입을 바꿀 계획이 있다** — DB가 EC2 수명에 묶이지 않는다
- **DB가 실제로 무거워진다** — 대량 데이터 적재나 무거운 집계를 돌릴 계획

바꾸는 건 어렵지 않다. RDS를 만들고 서버 `.env`에 접속정보를 넣은 뒤,
`docker-compose.prod.yml`에서 `mysql` 서비스와 `depends_on`을 지우고
`DB_URL`을 RDS 엔드포인트로 바꾸면 끝이다. 앱 코드는 한 줄도 안 바뀐다
(`application-prod.yml`이 이미 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` 환경변수만 본다).

> RDS를 만들 때 **파라미터 그룹에서 `time_zone`을 `Asia/Seoul`로 지정하는 걸 잊지 말 것.**
> 기본값은 UTC라서 `NOW()`가 9시간 이르게 찍힌다.

---

## 1. EC2 만들기 (최초 1회)

콘솔에서 만들어도 되고 CLI로 해도 된다. 스펙만 맞추면 된다.

| 항목 | 값 |
|---|---|
| AMI | Amazon Linux 2023 |
| 인스턴스 타입 | **t3.small** (2 GiB) |
| 스토리지 | gp3 20 GiB |
| 리전 | `ap-northeast-2` (서울) |
| 키페어 | 새로 만들고 `.pem`을 받아둔다 — GitHub Secret으로 쓴다 |
| 퍼블릭 IP | 자동 할당 켬 |

> ⚠️ **t3.micro(1 GiB)는 피한다.** MySQL과 JVM을 한 박스에 올리면 메모리가 모자라
> 기동 중에 OOM으로 죽는다.

**메모리 배분** — 한 박스에 MySQL과 JVM이 같이 사니까 미리 나눠놨다.
`docker-compose.prod.yml`의 `mem_limit`이 t3.small(2 GiB) 기준으로 잡혀 있다.

| | 한도 | 비고 |
|---|---|---|
| 백엔드 | 900 MB | JVM 힙은 이 값의 75%(=675 MB)로 잡힌다 |
| MySQL | 640 MB | `performance_schema`를 꺼서 200~400MB를 아꼈다 |
| OS · 도커 | 나머지 ~500 MB | |

> `mem_limit`을 안 걸면 JVM이 **호스트 전체**의 75%(1.5 GiB)를 힙 최대치로 잡아서
> MySQL이 쓸 게 안 남는다. 그러면 커널 OOM killer가 둘 중 하나를 죽이는데,
> 시연 도중에 이게 터지면 손 쓸 방법이 없다.

**여유 있게 가고 싶으면 t3.medium(4 GiB)**을 고른다. 시간당 $0.052라 3일에 $3.7이고,
**RDS를 붙이는 것과 비용이 비슷한데 세팅할 게 하나도 안 늘어난다.**
그때는 위 `mem_limit`을 각각 2g / 1g 정도로 올린다.

**보안 그룹**

| 포트 | 소스 | 용도 |
|---|---|---|
| 22 | 내 IP | SSH |
| 80 | 0.0.0.0/0 | API |

3306은 **열지 않는다.** 백엔드 컨테이너가 도커 네트워크 안에서만 MySQL에 붙는다
(`docker-compose.prod.yml`이 `ports` 대신 `expose`를 쓰는 이유). 3306을 인터넷에 노출하면
봇이 몇 시간 안에 스캔해 들어온다.

---

## 2. 서버 초기 세팅 (최초 1회)

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

# 스왑 2GB. 메모리가 순간적으로 튈 때 프로세스가 죽는 대신 느려지고 만다.
# t3.small처럼 빠듯한 인스턴스에서는 이게 있고 없고가 크다.
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab   # 재부팅 후에도 유지

# 그룹 변경을 반영하려면 다시 접속해야 한다
exit
```

다시 접속해서 배포 디렉터리와 `.env`를 만든다.

```bash
mkdir -p ~/dday && cd ~/dday

cat > .env <<'EOF'
MYSQL_ROOT_PASSWORD={길고 랜덤한 값}
MYSQL_DATABASE=dday
MYSQL_USER=dday
MYSQL_PASSWORD={길고 랜덤한 값}
CORS_ALLOWED_ORIGINS=
EOF
chmod 600 .env
```

> **이 `.env`는 서버에만 있고 저장소에는 없다.** Actions는 `docker-compose.prod.yml`만
> 덮어쓰고 `.env`는 건드리지 않으므로, 한 번 만들어두면 배포할 때마다 유지된다.
> 비밀번호를 잃어버리면 DB 볼륨을 날리고 다시 만드는 수밖에 없다.

---

## 3. GitHub 설정

**Settings → Secrets and variables → Actions**

| 종류 | 이름 | 값 |
|---|---|---|
| Secret | `EC2_HOST` | EC2 퍼블릭 IP |
| Secret | `EC2_USER` | `ec2-user` |
| Secret | `EC2_SSH_KEY` | `.pem` 파일 **내용 전체** (`-----BEGIN ...` 줄 포함) |
| Variable | `DEPLOY_ENABLED` | `true` |

`DEPLOY_ENABLED`는 **Variables 탭**이다 (Secrets 아님). 이 값이 `true`가 아니면
`deploy` 잡을 건너뛴다.

### ⚠️ GHCR 패키지를 public으로 바꿔야 한다 (최초 1회)

이미지를 처음 push하고 나면 GitHub에 `dday-backend` 패키지가 생기는데,
**저장소가 public이어도 패키지는 기본이 private다.** 그대로 두면 EC2에서
`docker compose pull`이 `denied`로 실패한다.

첫 push 후 한 번만:
**저장소 → Packages → `dday-backend` → Package settings → Danger Zone →
Change visibility → Public**

(공개하기 싫으면 대신 서버에서 `echo {PAT} | docker login ghcr.io -u {계정} --password-stdin`을
해두면 된다. `read:packages` 권한의 PAT가 필요하다.)

---

## 4. 첫 배포

Actions 탭 → CI → **Run workflow** → branch `main`.
코드를 안 바꿔도 배포까지 그대로 이어진다. 배선만 먼저 확인할 때 쓴다.

성공하면 워크플로 마지막 단계가 `/health/db` 응답을 찍는다:

```json
{"success":true,"code":"DB_HEALTHY","message":"데이터베이스 연결이 정상입니다.","data":1}
```

브라우저로도 확인:
- `http://{퍼블릭IP}/health/db`
- `http://{퍼블릭IP}/swagger-ui.html`

---

## 5. 안 될 때

```bash
ssh -i ~/받은키.pem ec2-user@{퍼블릭IP}
cd ~/dday

docker compose -f docker-compose.prod.yml ps          # 뭐가 떠 있나
docker compose -f docker-compose.prod.yml logs -f backend   # 앱 로그
docker compose -f docker-compose.prod.yml logs mysql        # DB 로그
```

| 증상 | 원인 |
|---|---|
| `pull` 이 `denied` | GHCR 패키지가 아직 private다 (§3) |
| 백엔드가 계속 재시작 | `.env`의 DB 비밀번호가 이미 만들어진 볼륨과 다르다. 로그에 `Access denied` |
| `/health`는 되는데 `/health/db`가 500 | MySQL 컨테이너가 아직 안 떴거나 DB 접속정보가 틀렸다 |
| 응답 자체가 없음 | 보안 그룹에 80이 안 열렸거나 컨테이너가 안 떴다 |

### 볼륨을 날리고 새로 시작

```bash
docker compose -f docker-compose.prod.yml down -v
docker compose -f docker-compose.prod.yml up -d
```

**DB 데이터가 전부 사라진다.** 시연 직전엔 하지 않는다.

---

## 6. 롤백

이미지에 커밋 sha 태그가 같이 붙어 있어서 이전 커밋으로 되돌릴 수 있다.

```bash
cd ~/dday
docker compose -f docker-compose.prod.yml down backend
docker run -d --name dday-backend --network dday_default -p 80:8080 \
  --env-file .env -e SPRING_PROFILES_ACTIVE=prod \
  ghcr.io/26kb-hackathon-dday/dday-backend:{되돌릴 커밋 sha}
```

간단하게는 **되돌리고 싶은 커밋으로 revert 커밋을 만들어 main에 push**하면
파이프라인이 알아서 다시 배포한다. 이쪽이 덜 헷갈린다.

---

## 7. 비용

`ap-northeast-2` 기준.

| 항목 | 시간당 | 3일 |
|---|---|---|
| t3.small | $0.0260 | $1.87 |
| 퍼블릭 IPv4 1개 | $0.0050 | $0.36 |
| gp3 20GiB | ~$0.0027 | $0.19 |
| **합계** | **~$0.034** | **~$2.4** |

> 퍼블릭 IPv4는 2024-02-01부터 **붙어 있기만 해도** $0.005/hr다.
> "쓰고 있으면 무료"는 폐지된 규칙이라 비용 계산에서 빼먹기 쉽다.

다른 선택지와 비교하면 (3일 기준):

| 구성 | 3일 비용 | 추가 세팅 |
|---|---|---|
| **t3.small + compose MySQL** (현재) | ~$2.4 | 없음 |
| t3.medium + compose MySQL | ~$4.3 | 없음 (인스턴스 타입만 다름) |
| t3.small + `db.t4g.micro` RDS | ~$4.2 | 서브넷/보안/파라미터 그룹, 20~30분 |

**여유가 필요하면 RDS보다 t3.medium이 먼저다.** 같은 돈에 세팅이 안 늘어난다.
RDS는 "데이터가 EC2보다 오래 살아야 할 때" 고르는 것이지, 성능이나 비용 때문이 아니다.
(정확한 단가는 리전·시점에 따라 바뀌니 확정 전에 AWS 요금 페이지로 확인할 것)

**해커톤이 끝나면 인스턴스를 terminate한다.** stop만 하면 EBS 요금이 계속 나간다.

---

## 8. 이 구성이 안 하는 것

- **HTTPS가 없다.** 도메인이 없으면 인증서를 받을 수 없다

  > ⚠️ **프론트를 Vercel/Netlify 같은 HTTPS 호스팅에 올리면 이 백엔드를 호출하지 못한다.**
  > HTTPS 페이지에서 HTTP API를 부르는 건 브라우저가 mixed content로 차단한다.
  > 셋 중 하나를 골라야 한다:
  > ① 프론트도 같은 EC2에 http로 올린다 (가장 간단, 데모엔 충분)
  > ② 도메인을 하나 사서 Let's Encrypt로 인증서를 붙인다 (1~2시간)
  > ③ 프론트를 로컬에서 띄워 시연한다
  >
  > **프론트 배포 위치를 정할 때 이걸 먼저 확인한다.** 마지막 날에 발견하면 늦는다.

- **DB 백업이 없다.** 인스턴스를 날리면 데이터도 같이 사라진다.
  시연에 꼭 필요한 데이터가 생기면 `db/seed.sql` 같은 스크립트로 저장소에 넣어둔다
- **무중단 배포가 아니다.** `up -d`가 컨테이너를 갈아끼우는 몇 초 동안 502가 난다.
  시연 중에는 main에 push하지 않는다
- **모니터링이 없다.** 로그는 `docker compose logs`로 직접 본다
