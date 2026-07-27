# Chaos 실행 런북 — 장애 주입으로 RCA 품질 검증

> AI 기반 RCA(trace · Loki · metrics로 원인 분석)의 품질을 측정하려면, **원인을 내가 알고 있는 실제 장애**가 필요하다.
> 그래서 각 문항은 실제 인프라 레벨 주입 + 정답지(원인 1줄) + 근거 시그널 + 복구 절차로 구성하고,
> 채점은 블라인드로 한다(§8). "실제로 해봤는가 / 어떻게 평가하는가"에 대한 답이 이 문서 전체다.
> 모든 문항은 **정상 측정 → 기록 → 주입 → 증상 대조 → 원복 → RCA 채점** 사이클(§3.3)로 실행한다 — 정상이 측정되지 않으면 장애도 판정할 수 없다.
> 이 문서만 보고 처음부터 끝까지 실행할 수 있게 작성한다. 마지막 갱신: 2026-07-24.

## 설계 원칙

1. **주입은 실제 장애와 같은 텔레메트리를 남긴다** — Thread.sleep 심는 합성 chaos 코드 지양. 인프라 레벨 주입(docker stop / kubectl scale / 리소스 limit)을 우선하고, 코드 변경은 "계측 구멍 메우기"에만 쓴다. (근거: §11 주입 방식 결정 근거)
2. **정답 시그널이 관측 가능한지 먼저 확인** — 시그널이 안 잡히는 문항은 AI가 못 맞추는 게 아니라 문항이 성립 안 하는 것. 전제 작업(§10)이 그 조건이다.
3. **증상→원인 거리(hop)를 다양하게** — 1-hop(AU-3)부터 다중 서비스 복합(IN-1)까지 섞어야 변별력 있는 문항 세트가 된다.
4. **정상 먼저, 주입은 그다음** — 문항마다 주입 직전에 증상 관측과 **같은 쿼리**로 정상값을 채록한다(§3.3 사이클). 정상 상태에서 쿼리가 비면 그 문항은 성립하지 않는 것 — 원칙 2를 문항마다 게이트로 강제하는 장치다.

## 0. 사전 조건

실행 전에 반드시 완료되어 있어야 하는 것.

- [ ] §10 사전 작업 Step 0 완료 — 완료 전에는 "레포 기준 원복" 수칙이 성립하지 않는다(레포본이 배포되면 서비스가 죽는 상태였음).
- [x] 배포 방식 확인(2026-07-24): ArgoCD 미사용 — GHCR 이미지 pull 방식이라 kubectl patch류 주입이 selfHeal로 자동 원복되지 않는다. 뒤집어 말하면 원복도 아무도 대신해 주지 않으니, 원복 책임은 전부 런북 원복 줄에 있다(§2.3).
- [ ] 저트래픽 시간대인지 확인. 한 번에 한 문항만 — 문항당 §3.3 사이클(① 정상 측정 ~ ⑤ 복귀 확인)을 완주한다.
- [ ] 세션 공통 baseline 채록(§3.3 하단) — 첫 문항 시작 전 1회.
- [ ] 로컬에 `jq`, `k6` 설치.

## 1. 변수 정의

아래 블록을 채워서 셸에 한 번 붙여넣으면, 이후 모든 명령은 복붙으로 실행된다. 어느 셸에서 붙여넣는지는 §2.1을 먼저 볼 것.

```bash
# ── 접속 (값은 로컬 메모에서 — 공개 문서에 기록 금지)
INFRA_SSH="ssh -i ~/<KEY>.pem ubuntu@<INFRA_PRIVATE_IP>"   # redis/kafka/mongo docker가 있는 인프라 노드 (bastion 경유 alias 사용 가능)

# ── 클러스터 (master 노드에서 실행)
NS=default                        # 서비스가 뜬 네임스페이스. 확인: kubectl get deploy -A
CONTENT_DEPLOY=content-service
AUTH_DEPLOY=auth-service
CHAT_DEPLOY=chat-service

# ── 인프라 컨테이너 이름. 확인: $INFRA_SSH "docker ps --format '{{.Names}}'"
REDIS_CT=<redis-container>
KAFKA_CT=<kafka-container>
MONGO_CT=<mongo-container>

# ── API (curl은 아무 셸에서나 — 공개 ingress)
BASE=https://yogurtte.com/api
EMAIL=<테스트 계정 이메일>          # 실사용자 계정 금지. k6용 시드 계정 재사용 가능 (toy-user scripts/ 참고)
PASSWORD=<비밀번호>

# ── 대상 콘텐츠 (§3에서 선정)
BATTLE_ID=
ITEM_ID=
FEED_ID=                          # AP-1·AP-3 댓글 대상 (내가 만든 테스트 피드 권장)
```

## 2. 실행 위치 · 측정 지점 · 원복 원칙

전제부터: **이 스택은 전부 k3s가 아니다.** 앱(content/chat/user)만 k3s 파드이고, MongoDB·Redis·Kafka는 별도 인프라 EC2의 docker 컨테이너다(configmap이 단일 인프라 사설 IP를 가리키는 게 증거). 그래서 `docker stop`은 kubectl이 아니라 그 인프라 노드의 순수 docker 명령이다. 명령마다 실행 위치가 갈린다.

### 2.1 어디서 무엇을 실행하나

| 명령 유형 | 예시 | 실행 위치 | 접근 방법 |
|---|---|---|---|
| kubectl (scale/patch/rollout/get) | `kubectl scale deploy/$AUTH_DEPLOY --replicas=0` | **master 노드** | kubeconfig가 master에 있음 |
| `$INFRA_SSH "docker ..."` | `$INFRA_SSH "docker stop $MONGO_CT"` | **master에서 타이핑** → SSH로 인프라 노드에 들어가 docker 실행 | 인프라 노드는 사설 IP라 VPC 안(master)에서만 닿음 |
| curl API | `curl $BASE/...` | **아무데서나**(로컬 Mac 포함) | 공개 ingress(`yogurtte.com`) |
| k6 부하 | `k6 run ...` | 로컬 또는 master | 공개 ingress 대상 |

즉 `date -u && $INFRA_SSH "docker stop $MONGO_CT"` 한 줄은 — `date`와 ssh 실행은 **master에서**, 그 ssh가 인프라 노드로 hop해서 `docker stop`은 **인프라 노드에서** 실행된다.

실행 전 두 가지를 확인한다(§10 Step 0과 연동):
- master → 인프라 노드 SSH가 뚫려 있나 — ① 인프라 노드 .pem 키가 master의 `~/`에 있는지, ② 보안그룹이 master → 인프라 22번을 허용하는지. 안 되면 인프라 노드에 직접 SSH로 들어가 docker 명령을 로컬 실행.
- `$INFRA_SSH "docker ps --format '{{.Names}}'"`로 실제 컨테이너 이름을 확인해 `$MONGO_CT`/`$REDIS_CT`/`$KAFKA_CT`에 반영.

### 2.2 어디서 측정하나

모든 시그널은 Alloy가 Grafana Cloud로 모아준다. 브라우저에서 Grafana 하나로 metrics·logs·traces를 다 본다.

| 시그널 | 도구 | 접근 |
|---|---|---|
| 메트릭 (P99, 5xx, HikariCP pending, consumer lag, active_users) | Grafana > Explore > Prometheus | 각 문항의 PromQL 복붙 |
| 트레이스 (span 잘림, DLQ 발행, 공백 구간) | Grafana > Explore > Tempo | `service.name` + `error=true` 필터, 주입 시각 이후 |
| 로그 (fallback, JwtFilter, DLQ, Retry) | Grafana > Explore > Loki | 각 문항의 `{service_name="..."}` LogQL |
| 대시보드 (Four Golden Signals, Saturation) | Grafana > Dashboards | baseline·증상 스크린샷 채록 |
| 파드 상태 (재시작, readiness) | **master**: `kubectl get pods -n $NS`, `kubectl describe pod` | 주입 반영·복구 확인 |
| 원시 actuator (디버그용) | master에서 port-forward 후 curl | 급할 때만 (아래) |

관리 포트(8090)는 외부 미노출이라 actuator를 직접 보려면 master에서 터널이 필요하다. 평상시 측정은 전부 Grafana로 한다.

```bash
# (디버그용) actuator 직접 확인 — master에서
kubectl -n $NS port-forward svc/$CONTENT_DEPLOY 8090:8090 &
curl -s localhost:8090/actuator/health/readiness | jq          # db·redis 포함
curl -s localhost:8090/actuator/prometheus | grep hikaricp_connections_pending
```

### 2.3 원복 원칙

- **원복 명령은 주입 전에 준비한다.** 각 §6 런북은 주입·트리거·원복을 한 블록에 담았으니, 주입 줄을 실행하기 전에 원복 줄을 먼저 셸에 붙여둔다.
- 주입 유형별 원복: `docker stop` → `docker start` / `scale --replicas=0` → `--replicas=1` + `rollout status` / `patch`(리소스) → 원래 값으로 재 patch / `secret` 변경 → 백업 `apply` + `rollout restart`.
- **복구는 "명령 실행"이 아니라 "정상화 확인"까지다.** `kubectl get pods -n $NS`에서 Running/Ready, readiness UP(§2.2 터널), 대표 트리거(T1 또는 T4) 200 복귀, 그리고 **그 문항 ①의 baseline 쿼리가 정상값으로 돌아온 것**(§3.3 ⑤)까지 본 뒤 다음 문항으로 넘어간다.
- kubectl로 직접 바꾼 것은 매니페스트 레포와 drift가 난다. 채록 후 `kubectl diff -k apps/<svc>/overlays/<env>`로 레포 기준과 일치하는지 확인한다(§10 Step 0 완료가 전제).

## 3. 공통 준비

### 3.1 토큰 발급

```bash
TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"'$EMAIL'","password":"'$PASSWORD'"}' | jq -r '.accessToken')
echo ${TOKEN:0:20}...   # 비어 있으면 로그인 실패
```

### 3.2 대상 배틀/아이템 선정

진행 중 배틀에 댓글을 달아 알림 파이프라인(content → Kafka → chat)을 태우는 것이 주력 트리거다.

```bash
curl -s "$BASE/content/battles/hot" -H "Authorization: Bearer $TOKEN" | jq '.data'   # 배틀 목록에서 BATTLE_ID 선정
curl -s "$BASE/content/battles/$BATTLE_ID" -H "Authorization: Bearer $TOKEN" | jq '.data'   # 아이템 목록에서 ITEM_ID 선정
curl -s "$BASE/content/feeds/scroll" | jq '.data.content[0].feedId'                          # AP 계열용 FEED_ID 선정
BATTLE_ID=<선정값>; ITEM_ID=<선정값>; FEED_ID=<선정값>
```

주의: 댓글 알림은 **아이템/배틀 작성자에게** 발송된다. 알림 도착까지 검증하려면 작성자 계정에 접근 가능한 배틀(내가 만든 테스트 배틀)을 쓴다.

### 3.3 문항 실행 사이클 — 정상 측정 → 기록 → 주입 → 증상 대조 → 원복 → RCA

카오스 문항은 "정상과의 차이"를 읽는 시험이다. 그래서 모든 문항은 아래 사이클로 돌고, §6의 각 런북이 이 순서(①~⑤ 라벨)로 구성돼 있다.

| 단계 | 하는 일 | 산출물 |
|---|---|---|
| ① 정상 측정 | 그 문항 ④의 관측 쿼리를 주입 **전에 그대로** 실행해 정상값 확인 | 정상값 · 정상 traceId |
| ② 기록 | ①의 결과를 `evidence/<ID>/baseline/`에 저장 | 스크린샷 · 쿼리 결과 |
| ③ 주입 + 트리거 | 런북 bash 블록. 주입 시각 `date -u` 기록, 원복 줄은 주입 전에 준비(§2.3) | 주입 시각(UTC) |
| ④ 증상 관측 | ①과 **같은 쿼리** 재실행 — 판정은 절대값이 아니라 ① 대비 변화 | `evidence/<ID>/symptom/` |
| ⑤ 원복 + 복귀 확인 | 원복 후 ① 쿼리 재실행 → 정상값 복귀까지가 한 문항(§2.3) | 복구 시각(UTC) |
| ⑥ RCA 채점 | 채록 창 + baseline 창을 입력으로 §8 블라인드 채점 — 회차마다(§8.1) | 회차별 RCA 점수 |

**① 게이트 규칙: 정상 상태에서 쿼리가 빈 값이면 주입 금지.** 메트릭 미노출(§10 전제 미충족)이거나 쿼리/라벨 오류다 — 어느 쪽이든 정상 상태에서 고치는 게 장애 와중에 디버깅하는 것보다 압도적으로 싸다. CH-2의 "보류" 판정도 이 게이트에서 난다.

①과 ④가 같은 쿼리라는 것이 핵심이다. 증상 때 신호가 안 보이면 "장애가 신호를 안 남긴 것"(계측 구멍 = 성과, §9)인지 "쿼리가 틀린 것"인지 갈라야 하는데, ①에서 이미 값을 봤다면 전자로 확정된다.

사이클 전 단계는 `docs/chaos/scripts/chaos.sh`(master에서 실행, 설정은 `chaos.env`)로 코드화되어 있다:

```bash
./chaos.sh <ID> baseline   # ①+② — 쿼리 실행·evidence 저장. 게이트 실패(빈 값) 시 exit 1 = 주입 금지
./chaos.sh <ID> on         # ③ 주입 (주입 시각 timeline.log 기록)
./chaos.sh <ID> trigger    # ③ 트리거 루프 (CH-2, AU-1만 — 나머지는 symptom에 T1/T2 포함)
./chaos.sh <ID> symptom    # ④ — baseline과 같은 함수 재실행 (①=④ 동일 쿼리가 구조적으로 보장)
./chaos.sh <ID> off        # ⑤ 원복 + 복귀 확인 폴링
./chaos.sh <ID> run        # ①~⑤ 전체 — 단계마다 확인 프롬프트, Ctrl-C 시 trap 자동 원복
```

자동화 범위 밖(사람 몫): Tempo 트레이스 **모양** 판독(3s 잘림 vs 즉시 실패), 알림 실제 도착 확인, 판정 체크박스, ⑥ 블라인드 RCA. 스크립트가 `[수동]` 라벨로 그 목록을 출력한다.

세션 공통 baseline (첫 문항 시작 전 1회 — 문항별 ①과 별개):

1. Grafana 홈 대시보드(Four Golden Signals) 정상 상태 스크린샷.
2. 정상 트레이스 1개: T1(§4)을 1회 실행하고, Tempo에서 `service.name = content-service`, 최근 5분으로 검색해 대표 traceId 기록.
3. 주입 시각을 UTC로 기록할 준비(`date -u`).

## 4. 트리거 API 목록

문항들이 공용으로 쓰는 트리거. `<ID>`는 문항 ID로 치환해 로그에서 역추적 가능하게 한다. 전부 공개 ingress라 아무 셸에서나 실행된다.

| 코드 | 목적 | 명령 |
|---|---|---|
| T1 | 알림 파이프라인 1건 (댓글 작성) | 아래 참조 |
| T2 | 읽기 경로 (피드 스크롤, 비인증 가능) | `curl -s "$BASE/content/feeds/scroll" \| jq '.data.content[0].feedId'` |
| T3 | 로그인 경로 (auth 왕복) | §3.1과 동일. 반복 시 for 루프 |
| T4 | 인증 필수 경로 (JWT 검증 확인용) | `curl -s -o /dev/null -w '%{http_code}\n' "$BASE/content/feeds/following" -H "Authorization: Bearer $TOKEN"` |
| T5 | 부하 (커넥션 풀 압박) | `k6 run k6/single-test.js -e BASE_URL=$BASE/content -e TOKEN=$TOKEN` (스크립트 상단의 env 키 확인) |
| T6 | 쓰기 경로 (배틀 투표 — 트리거 편중 방지) | 아래 참조 |

```bash
# T1 — 댓글 작성. 40자 제한. 성공 시 200 + "코멘트가 등록되었습니다."
curl -s -X POST "$BASE/content/battles/$BATTLE_ID/items/$ITEM_ID/comments" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"content":"chaos-<ID>-'$(date +%H%M%S)'"}' | jq '.message'

# T6 — 배틀 투표 (쓰기 경로). 트리거가 T1(댓글→알림)에 편중되는 것을 막는 용도 —
# content 트래픽의 주력은 배틀·피드인데 문항 트리거가 알림 파이프라인에 몰려 있었다.
# 인프라 문항(IN-1, IN-3)에서 읽기(T2)·알림(T1)과 함께 쓰기 경로를 같이 채록한다.
# 재투표 정책상 2회째는 4xx일 수 있음 — 경로 커버 목적엔 1건이면 족하고, 코드·메시지를 그대로 채록.
curl -s -X POST "$BASE/content/battles/$BATTLE_ID/items/vote" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"votes":[{"itemId":'$ITEM_ID',"rank":1}]}' | jq '.message'
```

## 5. 문항 카탈로그

| ID | 시나리오 (정답지) | 주입 | hop | 전제 (§10) |
|---|---|---|---|---|
| CH-1 | MongoDB 다운 → chat 컨슈머 실패 → 재시도 3회 → DLQ | `docker stop $MONGO_CT` | 2 | chat Step 0 배포 검증 |
| CH-2 | chat 다운 → 컨슈머 lag 누적 → 복구 후 알림 몰아서 도착 | `kubectl scale --replicas=0` | 2 | lag 메트릭 — kafka-exporter로 충족(2026-07-26) |
| AU-1 | auth CPU 기아 → 응답 지연 → content 캐시 미스 시 fallback | cpu limit 50m patch | 2~3 | (권장) auth JDBC 계측 |
| AU-2 | auth 완전 다운 → 로그인 502, content는 전부 익명 사용자 | `kubectl scale --replicas=0` | 2 | 없음 |
| AU-3 | JWT 시크릿 드리프트 → content 전 인증 API 401 (로그인은 성공) | Secret 변경 + restart | 1 | 없음 |
| AU-4 | auth 다운 + user 캐시 만료 → content 작성자 익명 fallback (500이면 붕괴) | `kubectl scale --replicas=0` + 10분 유지 | 2~3 | user 캐시 TTL 10분 |
| IN-1 | Redis 다운 → 3개 서비스 동시 이상 | `docker stop $REDIS_CT` | 다중 | 스케줄러 @Observed 보강 |
| IN-2 | Kafka 다운 → 댓글 성공, 알림 조용히 유실 | `docker stop $KAFKA_CT` | 2 | 없음 |
| IN-3 | 커넥션 풀 고갈 → pending 적체 → 전면 지연 | k6 + 슬로우 쿼리 | 2 | Alert P0 룰 실구현 |
| IN-4 | Pod OOMKilled (보류) | heap 부하 | 2 | kube-state-metrics 활성화 전까지 보류 |
| AP-1 | 댓글 DTO @Size 부재 → 201자가 varchar(200) 위반 → 500 | 250자 실요청 | 1 | FEED_ID(§3.2) |
| AP-2 | 대용량 업로드 실패 — 계층 판별(ingress / multipart / 앱 미매핑) | 대용량 실요청 | 1~2 | 없음 |
| AP-3 | 4바이트 이모지 → charset 불일치 시 Incorrect string value | 이모지 실요청 | 1 | FEED_ID — 조건부(불성립 가능) |

각 문항이 **무엇을 확인하려는 문항인지**(변별 포인트·함정·대비쌍) 한 표 요약은 [README의 "문항 설계 의도"](README.md#문항-설계-의도--무엇을-확인하려-했나) 참조.

## 6. 문항별 실행 런북

모든 문항은 §3.3 사이클 순서로 적었다: **① 정상 측정(주입 전) → ② 기록 → ③ 주입·트리거 → ④ 증상 관측(①과 같은 쿼리) → 판정(① 대비) → ⑤ 원복·복귀 확인**. ①을 건너뛰고 주입하지 말 것 — ①이 비면 그 문항은 게이트에서 멈춘다(§3.3). 각 단계는 `scripts/chaos.sh <ID> baseline|on|trigger|symptom|off|run`과 1:1이다(§3.3) — 아래 bash 블록은 스크립트가 실행하는 원명령이며, 수동 실행 시 상단 주석의 실행 셸(master / infra / 아무 셸)을 따르고 원복 명령을 주입 **전에** 셸 히스토리에 준비해둔다(§2.3).

### CH-1 — Mongo 다운 → 컨슈머 재시도 → DLQ

① 정상 측정 (주입 전, ④와 같은 쿼리):

- [ ] T1 1건 → 200 + 알림 도착. Tempo에서 content 발행 → chat consume → Mongo insert가 한 트레이스로 이어지는 **정상 traceId** 기록 — 증상 트레이스(재시도→DLQ)와 대조할 기준.
- [ ] Loki `{service_name="chat-service"} |= "DLQ"` 최근 1h = 0건 (이미 찍히고 있으면 그 원인부터 규명, 주입 보류).

② 기록: 위 결과를 `evidence/CH-1/baseline/`에 저장.

③ 주입 → 트리거 → ⑤ 원복:

```bash
# 주입 — master 셸 (docker stop은 인프라 노드에서 실행됨)
date -u && $INFRA_SSH "docker stop $MONGO_CT"
# 트리거 — 아무 셸: T1 1건
# 원복 — master 셸
$INFRA_SSH "docker start $MONGO_CT" && date -u
```

④ 증상 관측 (①과 같은 쿼리, 전부 Grafana):

- 댓글 API 응답: **200이어야 함** (발행은 AFTER_COMMIT 뒤, 실패는 컨슈머 쪽).
- Tempo: `service.name = chat-service`, `error = true`, 주입 시각 이후 — consume span 하위에서 Mongo insert 예외 → 재시도 → DLQ producer span까지 한 트레이스에 보이는지.
- Loki: `{service_name="chat-service"} |= "DLQ"` 및 `|= "Retry"` (라벨 값은 Grafana 라벨 브라우저에서 확인).
- 부수 증상: 채팅 메시지 전송/저장 실패 (Mongo 공유).

판정 (① 대비):

- [ ] 댓글 200 + 알림 미도착 (①에서는 도착했음)
- [ ] chat 트레이스에 FixedBackOff(1000ms × 3) 재시도 흔적 + DLQ 발행 span — ①의 정상 트레이스와 모양 대조
- [ ] Mongo span에 error 태그
- [ ] ⑤ 원복 후 ① 재실행: T1 알림 도착 복귀 + DLQ 로그 증가 멈춤
- [ ] 복구 후: DLQ 적재분 처리 방침 확인 — 재발행 수단이 없으면 그 사실을 answer.md 비고에 기록

**⑥ 원복 후 10분 관찰 — 영향 판정 (2026-07-27 신설, 회차 3부터)**

원복만으로 끝내지 않는다. **트리거 발사 시각 기준 10분**까지 알림 도착 여부를 관찰하고,
그 결과를 answer.md에 판정으로 남긴다. 이것이 채점의 영향 판정 입력이다.

```bash
# 트리거 시각 + 10분까지, 30초 간격으로 수신자 알림함 확인
#   chaos.env에 RECIPIENT_EMAIL/PASSWORD가 있으면 chaos.sh의 notif_inbox가 대신한다
# 교차 확인 — Mongo에 실제로 저장됐는지 (로그 부재 추론에 의존하지 말 것)
$INFRA_SSH "docker exec mongodb mongosh --quiet --eval '
  db.getSiblingDB(\"toychat\").user_notifications.find({createdAt:{\$gte:new Date(\"<트리거시각>\")}}).count()'"
```

- [ ] **10분 내 도착** → 판정: "지연 도착". 도착 시각과 지연 폭 기록
- [ ] **10분 내 미도착** → 판정: "**10분 내 미도착**". "유실"로 단정하지 않는다 — DLQ 무한
      재시도가 있어 그 이후 도착이 가능하므로, 관측 가능한 사실만 적는다
- [ ] 도착 판정 근거를 **로그 부재가 아니라 실물로** 잡는다 (Mongo 문서 존재 / 알림함 응답).
      회차 2는 "이후 실패 로그 0건"이라는 부재 추론에 의존했다 — 그 감점 판정의 토대가
      약했다는 것이 2026-07-27 채점에서 지적됐다

정답지 (갈래별 — 트리거 후 복구까지 잔여 시간이 드라이버 대기 30s 안/밖):

- **갈래 A (경계 안)**: "MongoDB 다운. 드라이버 대기 안에 복구돼 재시도 없이 통과 — 지연만, 10분 내 도착."
- **갈래 B (경계 밖)**: "MongoDB 다운. 컨슈머는 재시도 소진 후 DLQ 적재, 복구 후 재처리로 10분 내 도착."

### CH-2 — 컨슈머 정지 → lag 누적

전제: 알림 컨슈머의 lag 메트릭이 노출되어 있어야 한다. **충족 확인(2026-07-26)** — kafka-exporter가 `kafka_consumergroup_lag{consumergroup="notification-processors", topic="user.notifications"}`를 이미 노출 중이라(IN-2 발견 #7) §10의 앱 메트릭 작업(MicrometerConsumerListener) 없이 성립한다. 브로커 측 집계라 **컨슈머가 replicas=0인 동안에도 계속 보인다** — 앱 메트릭이었다면 주입 중 메트릭 자체가 사라져 누적 곡선을 못 그렸을 것. 단, 주입 중 이 시계열이 사라지면 그건 exporter가 빈 그룹을 누락하는 것 — 그 자체를 발견으로 기록한다.

① 정상 측정 (주입 전, ④와 같은 쿼리):

- [ ] PromQL `sum(kafka_consumergroup_lag{consumergroup="notification-processors", topic="user.notifications"})` — 값이 **존재하고 ≈0**. 빈 값이면 전제 미충족 → 주입 금지(§3.3 게이트).
- [ ] PromQL `websocket_active_users` 정상값 기록.
- [ ] T1 1건 → 알림 도착까지 소요 시간 기록 — 복구 후 "몰아서 도착"과 대조할 기준.

② 기록: 위 결과를 `evidence/CH-2/baseline/`에 저장.

③ 주입 → 트리거 → ⑤ 원복:

```bash
# 주입 — master 셸
date -u && kubectl -n $NS scale deploy/$CHAT_DEPLOY --replicas=0
# 트리거 — 아무 셸: 5분간 댓글 트래픽 누적
for i in $(seq 1 30); do
  curl -s -o /dev/null -X POST "$BASE/content/battles/$BATTLE_ID/items/$ITEM_ID/comments" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d '{"content":"chaos-CH2-'$i'"}'; sleep 10
done
# 원복 — master 셸
kubectl -n $NS scale deploy/$CHAT_DEPLOY --replicas=1 && kubectl -n $NS rollout status deploy/$CHAT_DEPLOY && date -u
```

④ 증상 관측 (①과 같은 쿼리):

- PromQL: `websocket_active_users` — ①의 정상값 → 0으로 급락 (주입 직후).
- PromQL: `sum(kafka_consumergroup_lag{consumergroup="notification-processors", topic="user.notifications"})` — 누적 증가(트리거 30건 ≈ +30), 복구 후 해소 곡선.
- content 쪽 트레이스는 **깨끗해야 함** — "발행은 됐는데 소비가 없다"를 lag로 지목하는 것이 채점 포인트.
- 복구 후: 알림 몰아서 도착 확인.

판정 (① 대비): [ ] lag ≈0(①) → 누적 → 해소 곡선 채록 [ ] content 트레이스 무결 [ ] 복구 후 알림 일괄 도착 — ①의 도착 소요 시간과 대조 [ ] ⑤ 원복 후 lag·active_users가 ① 값으로 복귀

정답지: "chat 컨슈머 전멸로 lag 누적. 발행측 정상, 복구 시 밀린 알림 일괄 처리."

### AU-1 — auth CPU 기아 → 조용한 성능·데이터 품질 저하

① 정상 측정 (주입 전, ④와 같은 쿼리):

- [ ] ④의 로그인 P99 PromQL 정상값 기록 + 로그인 1회 `%{time_total}` 기록.
- [ ] Tempo: content 트레이스에서 `GET user-service` client span의 정상 duration(수십 ms대) 기록 — "3s에서 잘림"의 대조 기준. 캐시 히트면 이 span이 아예 없으니 TTL 경과 후 요청에서 잡는다.
- [ ] Loki `{service_name="content-service"} |= "fallback"` 최근 1h = 0건.
- [ ] T2 응답의 작성자 이름이 실명인지 확인 ("사용자{id}" 아님).

② 기록: 위 결과를 `evidence/AU-1/baseline/`에 저장.

③ 주입 → 트리거 → ⑤ 원복:

```bash
# 주입 — master 셸
date -u && kubectl -n $NS patch deploy/$AUTH_DEPLOY --type=json \
  -p '[{"op":"replace","path":"/spec/template/spec/containers/0/resources/limits/cpu","value":"50m"}]'
kubectl -n $NS rollout status deploy/$AUTH_DEPLOY
# 트리거 — 아무 셸: 로그인 반복 + content 읽기
for i in $(seq 1 20); do curl -s -o /dev/null -w '%{time_total}s\n' -X POST $BASE/auth/login \
  -H 'Content-Type: application/json' -d '{"email":"'$EMAIL'","password":"'$PASSWORD'"}'; done
# content 캐시 미스 유도: user 캐시 TTL이 지나야 fallback이 보인다.
# TTL 값을 먼저 확인(content UserCacheStore/RedisConfig)하고, 채록 창을 TTL보다 길게 잡는다.
# 그동안 T2를 주기 실행: watch -n 30 'curl -s -o /dev/null "$BASE/content/feeds/scroll"'
# 원복 — master 셸
kubectl -n $NS patch deploy/$AUTH_DEPLOY --type=json \
  -p '[{"op":"replace","path":"/spec/template/spec/containers/0/resources/limits/cpu","value":"500m"}]'
kubectl -n $NS rollout status deploy/$AUTH_DEPLOY && date -u
```

④ 증상 관측 (①과 같은 쿼리):

- PromQL (로그인 P99): `histogram_quantile(0.99, sum by (le) (rate(http_server_requests_seconds_bucket{application="auth-service", uri="/login"}[5m])))` — 급등.
- Tempo: `service.name = content-service`, duration > 3s — `GET user-service` client span이 정확히 3s에서 잘리는 트레이스.
- Loki: `{service_name="content-service"} |= "fallback"` — ExternalUserApiClient의 fallback 로그.
- 사용자 가시 증상: 피드 작성자가 "사용자{id}"(익명)로 표시.
- **에러율(5xx)은 오르지 않아야 한다** — 이 문항의 변별 포인트.

판정 (① 대비): [ ] 로그인 P99 ① 대비 급등 [ ] 5xx 무변화 [ ] client span 수십 ms(①) → 3s 잘림 [ ] fallback 로그 0건(①) → 발생, 작성자 실명 → 익명 [ ] ⑤ 원복 후 P99·`time_total`이 ① 값으로 복귀

정답지: "auth CPU limit 과소로 응답 지연. content는 3s timeout + fallback으로 방어했으나 데이터 품질 저하."

### AU-2 — auth 완전 다운

① 정상 측정 (주입 전): AU-1 직후 실행이면 AU-1의 ⑤ 복귀 확인이 ①을 겸한다. 단독 실행 시 — [ ] 로그인 200 + `time_total` 정상 [ ] T2 작성자 실명. ② `evidence/AU-2/baseline/` 저장.

③ 주입 → 트리거 → ⑤ 원복:

```bash
# 주입 — master 셸
date -u && kubectl -n $NS scale deploy/$AUTH_DEPLOY --replicas=0
# 트리거 — 아무 셸
curl -s -o /dev/null -w '%{http_code}\n' -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"'$EMAIL'","password":"'$PASSWORD'"}'        # 기대: 502
curl -s "$BASE/content/feeds/scroll" | jq '.data.content[0]' # 캐시 히트분은 정상, 미스분은 익명
# 원복 — master 셸
kubectl -n $NS scale deploy/$AUTH_DEPLOY --replicas=1 && kubectl -n $NS rollout status deploy/$AUTH_DEPLOY && date -u
```

④ 증상 관측·판정 (① 대비): 로그인 200(①) → 502(ingress) / content는 5xx 없음 / content client span이 **timeout(3s)이 아니라 connection refused로 즉시 실패** — AU-1과의 구별 자체가 채점 포인트. ⑤ 원복 후 로그인 200 복귀 확인.

정답지: "auth 전면 다운. content는 캐시 히트분 정상 + 미스분 익명 — fallback 설계 검증."

### AU-3 — JWT 시크릿 드리프트 (config drift, 전 사용자 영향 — 가장 마지막에, 가장 짧게)

① 정상 측정 (주입 전, ④와 같은 쿼리):

- [ ] T4 → 200.
- [ ] ④의 401 rate PromQL 현재값 기록 (평시 401 노이즈 수준 — "급증" 판정의 기준선).

② 기록: 위 결과를 `evidence/AU-3/baseline/`에 저장.

③ 주입 → 트리거 → ⑤ 원복:

```bash
# 백업 (평문 시크릿 포함 — 채록 후 즉시 삭제) — master 셸
kubectl -n $NS get secret content-secret -o yaml > /tmp/content-secret.backup.yaml
# 주입 — master 셸
date -u && kubectl -n $NS patch secret content-secret \
  -p '{"stringData":{"JWT_SECRET":"chaos-au3-drift-value-not-a-real-secret-0000000000"}}'
kubectl -n $NS rollout restart deploy/$CONTENT_DEPLOY && kubectl -n $NS rollout status deploy/$CONTENT_DEPLOY
# 트리거 — 아무 셸
TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"'$EMAIL'","password":"'$PASSWORD'"}' | jq -r '.accessToken')   # 로그인은 성공해야 함
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/content/feeds/following" -H "Authorization: Bearer $TOKEN"  # 기대: 401
# 원복 — master 셸
kubectl -n $NS apply -f /tmp/content-secret.backup.yaml
kubectl -n $NS rollout restart deploy/$CONTENT_DEPLOY && kubectl -n $NS rollout status deploy/$CONTENT_DEPLOY
rm /tmp/content-secret.backup.yaml && date -u
# 정상화 확인: T4가 200으로 복귀
```

④ 증상 관측 (①과 같은 쿼리):

- PromQL (4xx 비율): `sum(rate(http_server_requests_seconds_count{application="content-service", status="401"}[5m]))` — 인증 API 전반 급증.
- **401은 4xx라 span error 태그가 안 붙는다** — trace로는 못 찾는 문항. 메트릭(401 rate)과 `{service_name="content-service"} |= "JwtFilter"` 로그로 도달해야 한다. trace 의존도가 낮은 문항을 섞는 목적.

판정 (① 대비): [ ] 로그인 성공 + content 전 인증 API 401 (T4 200 → 401) [ ] 401 rate ① 기준선 대비 급증 채록 [ ] JwtFilter 로그 채록 [ ] ⑤ 원복 후 T4 200 복귀 (런북 마지막 줄)

정답지: "content의 JWT_SECRET이 auth와 어긋남(config drift). 로그인은 되는데 아무것도 안 되는 상태."

### AU-4 — auth 완전 다운 + user 캐시 만료 (fallback 경로 실검증)

AU-2에서 파생 신설(2026-07-26). AU-2는 user 캐시(TTL 10분) 히트라 content가 실명을 그대로 서빙 — "auth 죽어도 content 무영향"까지만 보이고, **캐시가 없을 때 fallback이 실제로 버티는지**는 검증되지 않는다. AU-4는 캐시를 만료시켜 그 경로를 연다. 주입은 AU-2와 동일(auth `scale 0`), 차이는 **10분+ 유지**와 판정.

전제(§10): user 캐시 TTL = 10분 (`UserCacheStore.DEFAULT_TTL = Duration.ofMinutes(10)`). content fallback 존재 확인됨 (`ExternalUserInfoService.getUserInfo`가 절대 null 반환 안 함 → `createFallbackUserInfo` 익명 "사용자N"). `/feeds/scroll`은 `?size=` 필수 (미지정 시 NPE 500 — content `1e7df3f`로 수정, 그 전 chaos.sh t2가 오염원이었음).

① 정상 측정 (주입 전): [ ] 로그인 200 [ ] T2(`?size=10`) 작성자 실명. ② `evidence/AU-4/baseline/` 저장.

③ 주입 → 10분+ 대기 → ④ 채록 → ⑤ 원복:

```bash
# 주입 — master 셸
date -u && kubectl -n $NS scale deploy/$AUTH_DEPLOY --replicas=0
# 캐시 만료 대기 — 10분+ (그 전엔 캐시 히트라 AU-2와 구별 안 됨). 대기 중 주기적으로 T2를 쳐 실명→익명 전환 시점 관측 권장
curl -s "$BASE/content/feeds/scroll?size=10" | jq '.data.content[].userInfo.nickname'   # 만료 전: 실명 / 만료 후: "사용자N"
# 트리거 — 아무 셸 (10분 경과 후)
curl -s -o /dev/null -w '%{http_code}\n' -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"'$EMAIL'","password":"'$PASSWORD'"}'        # 기대: 503 (직접 경로)
curl -s "$BASE/content/feeds/scroll?size=10" | jq '.data.content[].userInfo.nickname'   # 기대: 익명 "사용자N" (fallback 정상) / 500이면 fallback 붕괴
# 원복 — master 셸
kubectl -n $NS scale deploy/$AUTH_DEPLOY --replicas=1 && kubectl -n $NS rollout status deploy/$AUTH_DEPLOY && date -u
```

④ 증상 관측·판정 (① 대비): 로그인 200(①) → 503 / **캐시 만료 후 T2 작성자가 "사용자N" 익명이면 fallback 정상, 500이면 fallback 붕괴(실버그)** / content client span에 error. ⑤ 원복 후 로그인 200 + 작성자 실명 복귀 확인.

> **회차 1 실측 정정 (2026-07-27)** — 이 절의 원문은 "content client span이 **3s timeout**
> (`Duration.ofSeconds(3)`) 후 fallback"이었으나 **틀렸다**. 실측은 **23.5ms connection refused**다.
> `.timeout(3s)`는 *대기 상한*이라 TCP RST로 즉시 거절되면 발동하지 않는다.
> 3s가 실제로 걸리는 건 auth가 *죽을* 때가 아니라 *느려질* 때(AU-1)다.
>
> 함께 정정: 찾을 span 이름은 `GET user-service`가 **아니라 `http get`**이고, 서비스 식별은
> `client.name=auth-service` 속성에 있다. auth 서버 span은 `http get /external/users`이며
> **baseline에는 있고 symptom에는 없다** — 이 출현/부재 대조가 이 문항의 핵심 판독이다.
>
> 실측 근거와 앵커 v3는 `scenarios/AU-4/answer.md`.

정답지: "auth 전면 다운 + user 캐시 만료 → content가 auth 직행 3s timeout 후 익명 fallback으로 저하. 원인은 auth 다운이고, content 500이면 fallback 붕괴(별개)." AU-2와의 구별(캐시 히트 vs 만료)이 채점 포인트.

### IN-1 — Redis 다운 (다중 서비스 복합, 최고 난도)

① 정상 측정 (주입 전, ④와 같은 쿼리):

- [ ] T2 정상 latency + 작성자 실명 확인.
- [ ] T6 투표 1건 → 응답 코드·시간 기록 — 쓰기 경로의 정상 기준. Redis 다운 시 이 경로가 아픈지 자체가 채록 대상이다.
- [ ] Tempo: content 트레이스에서 `GET user-service` client span **빈도**가 낮은 것 확인(캐시 히트 상태) — 주입 후 "직행 호출 급증"의 대조 기준.
- [ ] 스케줄러 @Observed span이 주기대로 찍히는지 + 최근 핫스코어 갱신 시각 기록 — 안 보이면 §10 전제 미충족, 주입 금지(§3.3 게이트).
- [ ] Loki `{service_name="chat-service"} |= "Redis"` 최근 1h = 0건.

② 기록: 위 결과를 `evidence/IN-1/baseline/`에 저장.

③ 주입 → 트리거 → ⑤ 원복:

```bash
# 주입 — master 셸 (docker stop은 인프라 노드에서 실행됨)
date -u && $INFRA_SSH "docker stop $REDIS_CT"
# 트리거 — 아무 셸: T2 반복 + T1 1건. 스케줄러 증상은 트리거 없이 수 분 대기(핫스코어 주기 도래).
# 원복 — master 셸
$INFRA_SSH "docker start $REDIS_CT" && date -u
```

④ 증상 관측 (①과 같은 쿼리 — 서비스별로 다르게 아픈 것을 단일 근원으로 수렴시키는 문항):

- content: user 캐시 실패 → auth 직행 호출 급증(Tempo에서 `GET user-service` client span 빈도), latency 상승. ShedLock 획득 실패 → 스케줄러 skip → **수 분 뒤 핫스코어 갱신 정체** (2차 지연 증상 — 스케줄러 @Observed 필요, §10).
- 쓰기 경로(T6 투표): Redis 다운에도 정상인지, 실패한다면 어느 지점인지 — 어느 경로가 아프고 어느 경로가 멀쩡한지의 **대비 자체가 이 문항의 변별**이다.
- chat: 온라인 디바이스 조회 실패 → WS/FCM 발송 이상. Loki `{service_name="chat-service"} |= "Redis"`.
- auth: 이메일 인증·위치 검색 실패.
- 채록 전 확인: content `UserCacheStore`가 Redis 예외를 삼키고 API 직행하는지, 요청까지 깨지는지 — **실동작이 정답지의 일부.**

판정 (① 대비): [ ] 3개 서비스 증상 각각 채록 [ ] 스케줄러 주기 도래에도 skip — ①의 주기·갱신 시각과 대조 [ ] 단일 원인으로 수렴 가능한 시그널 경로 확인 [ ] ⑤ 원복 후 캐시 히트·스케줄러 주기가 ① 상태로 복귀

정답지: "Redis 다운. 캐시·분산락·프레즌스·인증코드가 한꺼번에 무너지되 서비스별 증상이 다름."

### IN-2 — Kafka 다운 (조용한 유실 — 데이터 유실 동반, 허용 범위 먼저 결정)

① 정상 측정 (주입 전):

- [ ] T1 1건 → 200 + 알림 도착. Tempo에서 **producer span이 정상 트레이스의 어디에 붙는지** 기록 — `sendSafely`가 예외를 삼키면 증상 트레이스엔 아무것도 안 남으므로, "사라진 span"을 알아보려면 정상 위치를 먼저 알아야 한다.

② 기록: 위 결과를 `evidence/IN-2/baseline/`에 저장.

③ 주입 → 트리거 → ⑤ 원복:

```bash
# 주입 — master 셸 (docker stop은 인프라 노드에서 실행됨)
date -u && $INFRA_SSH "docker stop $KAFKA_CT"
# 트리거 — 아무 셸: T1 1건 (댓글은 200이어야 함)
# 원복 — master 셸
$INFRA_SSH "docker start $KAFKA_CT" && date -u
```

④ 증상 관측 (① 대비):

- 댓글 200 + 알림 영구 미도착 (재시도 없음 — 유실 복구 불가).
- Tempo: content producer span에 error 태그가 남는지 — **에러 경로에 error 태그가 남는지 검증과 직결.** `NotificationService.sendSafely`가 예외를 삼켜 **아무 데도 안 보인다**면 그 자체가 계측 구멍 발견 → 보강 후 재채록.
- chat: 채팅 발신 실패 (사용자 가시 증상).

판정 (① 대비): [ ] 댓글 200 [ ] ①의 producer span 위치와 대조해 error 태그/span 부재 확인 [ ] 알림 유실 확인(①에서는 도착) [ ] ⑤ 원복 후 T1 알림 도착 복귀

정답지: "Kafka 다운. AFTER_COMMIT 발행이 실패하며 알림만 조용히 유실 — API는 전부 정상."

### IN-3 — 커넥션 풀 고갈

① 정상 측정 (주입 전, ④와 같은 쿼리):

- [ ] PromQL `hikaricp_connections_pending{application="content-service"}` = 0 (메트릭 존재 확인 겸 — 빈 값이면 주입 금지, §3.3 게이트).
- [ ] 무부하 상태 P99 기록 — "전면 지연"의 대조 기준.
- [ ] T6 투표 1건 응답 시간 기록 — "부하와 무관한 쓰기 경로까지 지연"(전면의 실증)의 대조 기준.
- [ ] Grafana Alerting에서 P0 룰이 존재하고 평시 미발화 상태인지 확인(§10 전제).

② 기록: 위 결과를 `evidence/IN-3/baseline/`에 저장.

③ 주입: 별도 주입 없음 — 슬로우 쿼리 문항(기존 문항 2)과 T5(k6 부하)를 조합하면 자연 발생.

```bash
# 아무 셸 (공개 ingress 대상)
k6 run k6/single-test.js -e BASE_URL=$BASE/content -e TOKEN=$TOKEN
```

④ 증상 관측 (①과 같은 쿼리):

- PromQL: `hikaricp_connections_pending{application="content-service"} > 0` — P0 알람 발화 여부까지 채록 (05-22 계획의 "실 발화 테스트"를 이 문항으로 소화).
- Tempo: connection acquire 대기가 JDBC span **앞단의 공백**으로 나타남 — span이 없는 구간을 읽는 능력이 채점 포인트.

판정 (① 대비): [ ] pending 0(①) → >0 채록 [ ] P0 알람 발화 (평시 미발화는 ①에서 확인됨) [ ] 트레이스 공백 구간 확인 [ ] ⑤ 부하 종료 후 pending·P99가 ① 값으로 복귀

정답지: "커넥션 풀 고갈. 슬로우 쿼리가 커넥션을 점유해 무관한 API까지 전면 지연 — DB가 아니라 커넥션 대기가 원인."

### AP 계열 공통 — 코드 결함 문항 (인프라 무접촉)

AP 문항의 주입은 docker stop이 아니라 **경계값 실요청 1건**이다. 결함(검증 구멍·예외 매핑 구멍·한도 설정)은 코드에 이미 잠복해 있고, 실제 사용자가 보낼 수 있는 요청으로 그것을 발화시킨다. §11이 배제하는 앱 레벨 카오스는 "코드를 고쳐 장애를 흉내 내는 것"이고 AP는 코드를 건드리지 않는다 — 남는 텔레메트리가 실사용자가 겪을 모양 그대로라 §11 원칙과 충돌하지 않는다.

인프라 문항과 질문이 다르다: "무엇이 죽었나"가 아니라 **"인프라가 전부 정상인데 왜 이 요청만 실패하나"**. 그래서 판정에 "정상 요청은 여전히 200"이 포함된다 — 전면 장애가 아님을 확인해야 요청 내용으로 시선이 간다.

- 원복이 없다 — 실패 요청은 트랜잭션 롤백으로 상태 무변화. `off`는 정상 요청 복귀 확인(+테스트 데이터 정리 메모)만 한다.
- **채록 → 블라인드 채점 → 보강 커밋 순서 엄수.** 결함을 먼저 고치면 문항이 소멸한다. 보강 커밋(@Size 추가·예외 매핑·업로드 검증)이 이 계열의 최종 산출물 — §9 "Then의 실패가 곧 성과"와 같은 서사.
- 전제: §3.2에서 `FEED_ID` 선정(AP-1·AP-3 댓글 대상 — 내가 만든 테스트 피드 권장).

### AP-1 — 댓글 201자: DTO 검증 구멍 → varchar(200) 위반 → 500

결함 위치(정답지의 근거): `FeedCommentRequest.CommentCreate`는 `@NotBlank`만 있고 `@Size`가 없다. 엔티티 `FeedComment.content`는 `length=200`. 201자 요청이 컨트롤러 검증을 통과해 INSERT에서 `DataIntegrityViolationException`(Data too long) → `GlobalExceptionHandler`에 미매핑이라 `handleAllException` → **500**.

① 정상 측정 (주입 전, ④와 같은 쿼리):

- [ ] 200자 이내 정상 댓글 → 200 (게이트: 실패하면 FEED_ID/토큰/서비스부터 규명, 주입 금지).
- [ ] Loki `{service_name="content-service"} |= "Data too long"` 최근 1h = 0건.
- [ ] ④의 500 rate PromQL 현재값 기록.

② 기록: `evidence/AP-1/baseline/`.

③ 주입 — 250자 댓글 1건 (아무 셸, 공개 ingress):

```bash
LONG=$(printf 'a%.0s' $(seq 1 250))
curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE/content/feeds/$FEED_ID/comments" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"content":"'$LONG'"}'          # 기대: 500
# 원복 없음 — 실패 INSERT는 롤백되어 상태 무변화
```

④ 증상 관측 (①과 같은 쿼리):

- 경계값 요청만 500, **정상 댓글은 여전히 200** — 전면 장애가 아니라는 것이 변별 포인트.
- Loki: `Data too long` / `DataIntegrityViolationException`.
- Tempo: content-service error 트레이스 — INSERT JDBC span에 error 태그.
- 500 rate: 단발 요청이라 rate에는 거의 안 잡힘 — **rate 기반 알람이 못 보는 유형의 장애**라는 사실 자체를 기록.

판정 (① 대비): [ ] 정상 댓글 200 유지 + 250자만 500 [ ] Loki 0건(①) → 발생 [ ] INSERT span error [ ] ⑤ 정상 댓글 200 재확인

정답지: "댓글 DTO에 @Size 부재 — 201자가 검증을 통과해 varchar(200) 제약 위반. DB가 아니라 앱 검증 구멍이 원인(+DataIntegrityViolation 미매핑으로 400 아닌 500)."

오귀인 함정: 로그의 SQL 예외만 보고 "DB 장애"로 지목하면 감점 — DB는 제약을 정확히 지켰다.

### AP-2 — 첨부 대용량 업로드: 실패 계층 판별 (ingress vs multipart vs 앱)

결함 위치: `spring.servlet.multipart.max-file-size: 1GB`(사실상 무제한) + 앱 레벨 크기·MIME 검증 전무 + `MaxUploadSizeExceededException` 미매핑(413이 아니라 500). 별도 잠복 버그: `FileService.uploadFile`이 경로 구분자 `"\\"`를 하드코딩 — Linux에서 업로드 자체가 깨질 수 있고, 이는 ① 게이트에서 실증된다.

① 정상 측정 (주입 전, ④와 같은 쿼리):

- [ ] 1KB 파일 업로드 → 200 + fileId. **500이면 잠복 버그(경로 구분자) 실증 — 문항 이전에 실버그 발견. answer.md에 기록하고 중단.**
- [ ] Loki `{service_name="content-service"} |= "MaxUploadSizeExceededException"` 최근 1h = 0건.

② 기록: `evidence/AP-2/baseline/`.

③ 주입 — 크기를 단계적으로 올리며 업로드 (2MB → 통과 시 1100MB. 대용량은 대역폭 부하 — 저트래픽·1회만):

```bash
dd if=/dev/zero of=/tmp/chaos-ap2.bin bs=1M count=2      # 통과(200)하면 count를 올려 재시도
curl -s -o /tmp/chaos-ap2-resp.txt -w '%{http_code}\n' -X POST "$BASE/content/attachment-file/upload" \
  -H "Authorization: Bearer $TOKEN" -F "file=@/tmp/chaos-ap2.bin"
cat /tmp/chaos-ap2-resp.txt      # 응답 본문이 nginx HTML인지 앱 JSON인지 = 계층 판별
rm /tmp/chaos-ap2.bin
```

④ 증상 관측 — **어느 계층이 거부했는지가 문항의 전부**:

| 관측 | 판정 |
|---|---|
| 413 + nginx HTML 본문 + 앱 로그·트레이스 **없음** | ingress body limit — 요청이 앱에 도달하지 않음 |
| 500 + 앱 JSON + Loki에 MaxUploadSizeExceededException | Spring multipart 한도 + 예외 미매핑 |
| 200 | 한도까지 전부 통과 — "1GB 무제한"이 실측으로 확인된 것. 그 자체를 기록 |

판정 (① 대비): [ ] 1KB는 200 유지 [ ] 실패 계층 판별 근거(응답 본문 + 앱 시그널 유무) 채록 [ ] ⑤ 성공 업로드분(200) fileId·서버 파일 수동 정리

정답지: "업로드 실패는 계층 문제 — 앱 시그널이 없으면 ingress body limit, 있으면 multipart 한도 + 미매핑 500. 근본 원인은 앱 레벨 크기·타입 검증 부재."

### AP-3 — 이모지 댓글: charset 불일치 (조건부 — 불성립 판정도 산출물)

결함 후보: `ddl-auto: update`로 테이블 charset이 DB 서버 기본값에 의존한다. utf8(3byte)로 생성됐다면 4바이트 이모지 저장 시 `Incorrect string value` → 500. utf8mb4라면 정상 저장 — **문항 불성립이고, charset이 검증됐다는 그 기록이 산출물**이다.

① 정상 측정: [ ] ASCII 댓글 → 200 (게이트) [ ] Loki `{service_name="content-service"} |= "Incorrect string value"` 1h = 0건. ② `evidence/AP-3/baseline/`.

③ 주입 — 이모지 댓글 1건:

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE/content/feeds/$FEED_ID/comments" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"content":"chaos-AP3-😀🎉"}'
# 200 → utf8mb4 확인, 문항 불성립 종료 (answer.md에 기록)
# 500 → 증상 채록 진행
```

④ 증상 관측 (500인 경우, ①과 같은 쿼리): Loki `Incorrect string value` — **AP-1과 같은 API·같은 500이지만 로그 지문이 다르다**(길이 vs 인코딩). 이 구별이 채점 포인트.

판정: [ ] ASCII 200 유지 [ ] 이모지 200(불성립 기록) 또는 500 + Incorrect string value 채록 [ ] ⑤ 테스트 댓글 정리 메모

정답지: "테이블 charset이 utf8(3byte)라 4바이트 문자 저장 불가 — 길이가 아니라 인코딩 문제. 조치는 utf8mb4 마이그레이션."

## 7. 실행 순서와 안전

### 7.1 권장 순서

증상→원인 거리(hop)가 짧고 주입이 단순한 것부터. 데이터 유실·전 사용자 영향 문항은 마지막에. 문항당 §3.3 사이클을 완주(⑤ 복귀 확인)한 뒤에만 다음으로 — 이전 문항의 잔여 증상이 다음 문항의 ① baseline을 오염시키면 안 된다.

1. **AP-1 → AP-3** (코드 결함 경계값) — 인프라 무접촉·원복 없음. 런북·토큰·FEED_ID 점검 겸 워밍업.
2. **AU-2** (auth scale 0) — 인프라 문항 중 주입/원복이 가장 단순.
3. **AU-1** (auth cpu 50m) — AU-2와 트레이스 구별(즉시 실패 vs 3s 잘림)을 연달아 채록.
4. **CH-1** (mongo 다운) — DLQ 경로.
5. **IN-2** (kafka 다운) — 조용한 유실. 유실 허용 범위 사전 결정.
6. **IN-1** (redis 다운) — 다중 서비스 복합, 최고 난도.
7. **IN-3** (커넥션 풀) — 부하 필요.
8. **AP-2** (대용량 업로드) — 대역폭 부하가 있어 부하 문항 옆에. 저트래픽·1회.
9. **AU-3** (JWT 드리프트) — 전 사용자 영향, **가장 마지막·가장 짧게**.
10. CH-2는 lag 메트릭 전제 충족 확인(2026-07-26, kafka-exporter) — 순서 제약 해제.

### 7.2 안전 수칙 (문항 실행 전 반드시 확인)

- **이 클러스터가 곧 실서비스다.** 저트래픽 시간대에, 문항당 주입 시간을 최소화(수 분)하고, 복구 검증까지가 한 문항이다.
- **원복 명령은 주입 전에 준비.** 각 런북의 원복 줄을 먼저 셸에 붙여두고 주입한다(§2.3).
- **selfHeal 자동 원복 없음 (확인됨 2026-07-24).** ArgoCD 미사용 — 배포는 GHCR 이미지 pull 방식이다. `kubectl patch/scale` 주입이 되돌아가지 않아 문항은 성립하지만, 원복도 아무도 대신해 주지 않는다. patch·scale 계열(AU-1, AU-2, AU-3, CH-2)은 원복 줄 실행을 절대 누락하지 말 것.
- **kubectl 직접 변경은 매니페스트와 drift.** 채록 후 즉시 원복하고, `kubectl diff -k apps/<svc>/overlays/<env>`로 레포 기준과 일치하는지 확인한다. 단, 레포가 낡아 있으면(§10 Step 0) 이 확인이 무의미하니 그 정리가 선행되어야 한다.
- **데이터 유실 동반 문항**(IN-2 Kafka, CH-1 Mongo): 알림 유실/지연 허용 범위를 먼저 결정하고 진행.

## 8. 블라인드 채점 — RCA 품질을 어떻게 평가하나

"어떻게 평가하죠"에 대한 답. 문항을 만들어 놓고 끝이 아니라, **정답지를 가린 채 RCA를 돌리고 정해진 기준으로 점수를 매긴다.** 채점자가 시나리오를 이미 아는 상태로 하는 자기 채점은 무효 — 그래서 블라인드다.

절차:

1. **입력 고정** — 각 문항의 채록 창(주입~복구 UTC 구간)과 직전 정상 창(§3.3 ①의 baseline), 대상 서비스만 RCA에 준다. 정답지·주입 명령은 가린다. 정상 창을 함께 주는 이유: 실전 RCA도 "평소와 무엇이 다른가"에서 출발하며, baseline 없이 절대값만으로는 이상 판정 자체가 흔들린다. **입력의 형식도 고정한다**(§8.2 입력 패키징).
2. **RCA 실행** — trace·Loki·metrics만으로 "원인 1줄 + 근거 경로"를 산출하게 한다(사람이든 AI든 동일 입력).
3. **채점** — 각 answer.md에 **채록 전에 박제해 둔 채점 앵커**(§8.2)와 대조해 점수화한다. 정답지와의 자유 대조가 아니라 앵커 기준 채점이다.
4. **반복** — 같은 문항을 §8.1 프로토콜로 2~3회 돌려 **평균 ± 편차**로 기록한다. 1회 점수는 인용하지 않는다.

| 항목 | 배점 | 만점 기준 |
|---|---|---|
| 근본 원인 정확도 | 40 | 정답지의 원인 1줄과 일치 (예: "Redis 다운"이지 "지연 발생"이 아님) |
| 근거 시그널 경로 | 30 | 어떤 트레이스/메트릭/로그를 어떤 순서로 봤는지가 정답지의 근거 경로와 일치 |
| 오귀인 없음 | 20 | 무관한 컴포넌트를 원인으로 지목하지 않음 (예: AU-1을 DB 문제로 오진하지 않음) |
| 조치 타당성 | 10 | 제시한 복구/완화가 실제 원복과 방향이 맞음 |

### 8.1 반복 프로토콜 — N=1은 측정이 아니라 일화다

- 문항당 **최소 2회, 권장 3회** 사이클(§3.3)을 완주하고 회차마다 독립 채점한다. 1회 점수로는 그 점수가 능력인지 우연인지 구분할 수 없다.
- 회차는 다른 날(최소 다른 세션)로 벌린다 — 같은 날 연속 실행은 채록 창이 겹치고 채점자 기억이 남는다.
- 점수는 **평균 ± 최대편차**로 결과 매트릭스(§9)에 기록한다. 회차 간 편차가 **±10을 넘으면 문항 불안정** — 원인(트리거 타이밍, 시그널 적재 지연, 앵커 모호)을 규명하기 전까지 그 문항의 점수는 인용하지 않는다.

### 8.2 채점 신뢰도 — 출제자가 채점자인 구조의 보정

정답지를 가려도 **정답지를 쓴 사람의 머리는 못 가린다.** 1인 운영에서 블라인드가 성립하려면 채점 재량을 구조로 줄여야 한다:

- **채점 앵커 선(先)박제**: 각 answer.md의 "채점 앵커" 표(항목별 만점/부분점/0점 기준)를 **채록 전에** 작성해 고정한다. 채록 후 앵커를 고치면 그 회차 채점은 무효 — 앵커 개정은 다음 회차부터 적용한다.
  - **개정 시 구버전을 지우지 않는다.** answer.md에 v1을 "보존" 절로 남기고 v2를 위에 둔다. 어느 회차가 어느 버전으로 채점됐는지 채점표에 함께 적는다 — 그래야 점수 변화가 성능 개선인지 앵커 변경인지 사후에 판별된다.
  - **앵커 개정 이력 (2026-07-27, v1 → v2)** — 최초 채점(CH-1 회차1·2, IN-2 회차1)에서 드러난 결함 4건을 반영. 상세는 rca-agent `docs/scoring/README.md`.
    ① **갈래별 앵커** — 전개가 갈리는 문항(CH-1: 드라이버 대기 안/밖, AU-2: 캐시 히트/미스)은 갈래마다 앵커를 둔다. v1이 한 갈래만 가정해 CH-1 회차 1이 채점 불가가 됐다.
    ② **부분점 3구간** — 만점/부분점(상)/부분점(하)/0점. v1은 2구간이라 "만점 요건 중 하나만 미달"과 "절반만 맞힘"이 같은 점수였다(40점 항목에서 낙폭 20점).
    ③ **오판 ≠ 미판단** — 미판단은 부분점(상), 확신 있게 틀린 오판은 부분점(하). 실무 위험도가 다르다.
    ④ **관측 불가능한 요건 제거** — 에이전트 입력에 없는 것(v0의 baseline 창)은 만점 요건에서 뺀다. 단 **수집 결함으로 못 보는 것은 남긴다** — 그게 개선 델타의 측정 지점이다.
- **영향 판정 용어 통일 (2026-07-27)**: "유실"이 아니라 **"10분 내 미도착"**으로 판정한다(기준 시각 = 트리거 발사). DLQ 무한 재시도가 있는 구조에서 "유실" 단정은 사후에 뒤집힐 수 있고, "10분 내 미도착"은 관측으로 항상 확정된다. 문항 간 영향 판정 능력을 같은 자로 비교하기 위해 전 문항 공통으로 쓴다. 도착 판정 근거는 **로그 부재 추론이 아니라 실물**(저장된 문서/알림함 응답)로 잡는다.
- **시간 분리**: 채록과 채점 사이 최소 하루(또는 별도 세션). 여러 문항을 몰아 채점할 땐 문항 순서를 섞는다.
- **자기 일치도 검사**: 동일한 RCA 출력을 이틀 이상 간격으로 2회 채점해 점수 차를 answer.md에 기록한다. **차이가 ±5를 넘으면 앵커가 모호한 것** — 앵커를 보강하고 재채점한다.
- **입력 패키징 고정 (AI 피험자)**: AI에 주는 입력을 형식까지 고정한다 — ① 창 범위: `timeline.log`의 주입~복구 구간 + 직전 baseline, ② 데이터: 해당 문항 `evidence/` 원본(`prom-*.json`·`loki-*.json`·`tempo-*.json`)과 대시보드 스크린샷, ③ 도구 접근 여부(Grafana 직접 질의 허용/불허). 회차 간 형식이 다르면 점수 차가 문항 난이도 차인지 입력 차인지 구분할 수 없다 — AI 점수는 문항 난이도보다 입력 포맷에 더 민감하다.

채점의 변별력이 곧 문항 세트의 가치다:

- **hop이 다양해야 점수가 갈린다** — 1-hop(AU-3)은 대부분 맞히고, 다중 서비스 복합(IN-1)에서 근거 경로 점수가 벌어진다. 그 격차가 RCA 능력의 지표.
- **"에러율이 안 오르는 장애"(AU-1)에서 오귀인 항목이 살아난다** — 메트릭만 보는 RCA는 여기서 감점된다. 이런 함정 문항이 루브릭을 의미 있게 만든다.
- **동일 문항 반복 시 점수가 재현되는지**도 본다 — 재현 안 되면 문항이 불안정한 것(§9의 재현 가능성).

이 절이 "어떻게 평가하죠"에 대한 방어다: 문항은 실제 주입(§6)으로 진짜였고, 채점은 정답지를 가린 루브릭으로 객관화했다.

## 9. 채록 산출물

문항 하나 = 테스트케이스 하나. Given/When/Then으로 패키징한다.

```
docs/chaos/
  README.md              # 인덱스 — 파일 지도 + 문항 카탈로그 + 빠른 시작
  COMMANDS.md            # 복붙용 실행 명령 — 문항별 baseline→on→(trigger)→symptom→off
  RUNBOOK.md             # 이 문서 — 설계 원칙·실행 위치·문항별 런북(§6)·채점(§8)·전제(§10)
  scripts/
    chaos.sh             # <ID> baseline|on|trigger|symptom|off|run — §3.3 사이클 실행기
    chaos.env.example    # 설정 템플릿 (복사해서 chaos.env로)
    chaos.env            # 실제 값 (gitignore — 시크릿·사설 IP)
  scenarios/<ID>/        # 문항 하나 = 테스트케이스 하나 (Given/When은 RUNBOOK.md §6)
    answer.md            # Then: 원인 1줄(정답지) + 근거 시그널 경로 + RCA 채점 결과
    evidence/            # chaos.sh가 자동 생성
      baseline/<ts>/     # ①·② 정상 측정 — 정상값·정상 traceId·대시보드 캡처
      symptom/<ts>/      # ④ 증상 — 캡처·에러 traceId·Loki 쿼리·알람 발화
      timeline.log       # 주입/원복 시각(UTC) — §8 블라인드 채점 입력
```

결과 매트릭스가 포트폴리오의 얼굴이다:

| 문항 | 주입 | 사용자 증상 | 근거 시그널 | 탐지 수단 | RCA 점수 (회차별 → 평균±편차) | 발견된 계측 구멍 → 보강 커밋 |
|---|---|---|---|---|---|---|

판단 근거(시니어 방어용):

1. **주입 충실도** — 앱 레벨 흉내(sleep/예외 assault)는 텔레메트리 모양이 실제 장애와 달라 무효. 인프라 레벨 주입만 쓴다(§11과 동일 논리). Then(기대 시그널)이 검증 대상인데 When이 가짜면 Then도 가짜다.
2. **재현 가능성이 테스트의 자격** — baseline·주입·원복·전제·트리거가 전부 코드/문서로 존재해야 "실험"이 아니라 "테스트". 같은 문항을 다시 돌려 같은 시그널이 안 나오면 그게 회귀다. (반복 횟수·편차 판정 기준은 §8.1)
3. **Then의 실패가 곧 성과** — 기대 시그널이 안 잡히면 문항 실패가 아니라 계측 구멍 발견이다(IN-2의 sendSafely 케이스). "장애를 주입해 관측성을 테스트하고, 실패한 assertion마다 보강 커밋이 나왔다"가 이 작업의 서사 — 시나리오 수보다 이 피드백 루프의 증거가 설득력이다.
4. **evidence 공개 주의** — 스크린샷에 사설 IP·내부 호스트명이 들어가지 않게 캡처 범위를 정한다(레포 공개 방침과 충돌 금지).

## 10. 사전 작업 (문항의 전제)

### Step 0 — 소스 오브 트루스 확인 (변경 없이, 모든 문항의 전제)

레포 base 이름(`content`/`chat`/`auth`/`user`)과 운영본 이름(`content-service` 등)이 다르고, 매니페스트 레포가 초기 스켈레톤에서 멈춰 있어 "레포 기준 원복"이 서비스를 죽일 수 있다. 아래로 live 사실을 먼저 확정한다(전부 master 셸).

```bash
kubectl get ns
kubectl get deploy,svc -A | grep -E 'content|chat|auth|user'   # 실제 이름·네임스페이스 → §1 변수에 반영
```

- [ ] `NS`, `*_DEPLOY` 변수를 live 이름으로 교체
- [ ] 로그인/인증 서비스의 실제 배포명 확정(`user` vs `auth-service` drift 해소)
- [ ] (권장) live 스펙을 매니페스트 레포에 역반영해 drift를 닫은 뒤 채록 시작

### 문항별 사전 작업

| 대상 | 작업 | 없으면 성립 안 되는 문항 | 크기 |
|---|---|---|---|
| toy-chat | ~~`MicrometerConsumerListener` 등록~~ **불요(2026-07-26)** — kafka-exporter의 `kafka_consumergroup_lag`로 대체 | CH-2 (lag 시그널) | 0줄 |
| toy-content | `FeedTrendingScheduler`·`ProductPopularityScheduler`에 `@Observed` | IN-1 스케줄러 증상 관측 | 2줄 |
| toy-user | `datasource-micrometer-spring-boot:1.1.1` 추가 (auth는 현재 JDBC span 전무) | AU-1/AU-2 auth 내부 원인 분해 | 의존성 1줄 |
| 인프라 | infra 노드 docker 컨테이너명·중지/기동 runbook 확인 (mongo/redis/kafka) | CH-1, IN-1, IN-2 주입 수단 | 문서 |
| 인프라 | Grafana Alert 룰 실구현 (P0 HikariCP → 5xx → P99) | IN-3 알람 발화 채록 | 대시보드 |

exporter(redis/mongodb-exporter)는 v1에서 불요 — 앱 쪽 시그널만으로 정답 도달이 가능하다. 유일한 예외였던 CH-2(lag)는 이미 떠 있는 kafka-exporter가 해결했다(2026-07-26 확인).

## 11. 주입 방식 결정 근거 — 왜 앱 레벨 카오스를 안 쓰나

앱 레벨 옵션(프로필/플래그, chaos-monkey-spring-boot)은 배제한다. 주입 방식이 텔레메트리 모양을 바꾸기 때문. 예: CM4SB latency assault를 `ExternalUserApiClient`에 걸면 sleep이 메서드 앞에 끼므로 HTTP client span은 여전히 빠르고 server span에 원인 불명 공백만 생긴다 — AU-1 정답지(3s에서 잘리는 client span)와 다른 트레이스가 된다. RCA 문항은 정답 근거가 텔레메트리 모양 그 자체라, 흉내 낸 장애는 모양이 다르면 무효다. Kafka 다운을 예외 assault로 흉내 내도 connection refused 스택·타 서비스 동시 증상·복구 후 lag 해소 같은 진짜 장애의 지문이 안 생긴다.

대신 토글은 운영 계층에서 만든다 — `scripts/chaos.sh <ID> baseline|on|trigger|symptom|off|run`(구현됨, §3.3). 각 문항의 주입이 3~4줄 kubectl/docker이므로 래핑만 하면 되고, `off`(복구)가 항상 코드로 존재하며 `run`은 중단(trap) 시 자동 원복까지 보장해 안전 수칙이 구조적으로 충족된다.

단, **AP 계열(§6)은 이 배제에 해당하지 않는다** — 코드를 바꿔 장애를 흉내 내는 게 아니라 코드에 이미 있는 결함을 경계값 실요청으로 발화시키는 것이라, 텔레메트리가 흉내가 아니라 실제 그 자체다.
