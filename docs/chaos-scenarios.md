# Chaos 실행 런북 — 장애 주입으로 RCA 품질 검증

> AI 기반 RCA(trace · Loki · metrics로 원인 분석)의 품질을 측정하려면, **원인을 내가 알고 있는 실제 장애**가 필요하다.
> 그래서 각 문항은 실제 인프라 레벨 주입 + 정답지(원인 1줄) + 근거 시그널 + 복구 절차로 구성하고,
> 채점은 블라인드로 한다(§8). "실제로 해봤는가 / 어떻게 평가하는가"에 대한 답이 이 문서 전체다.
> 이 문서만 보고 처음부터 끝까지 실행할 수 있게 작성한다. 마지막 갱신: 2026-07-21.

## 설계 원칙

1. **주입은 실제 장애와 같은 텔레메트리를 남긴다** — Thread.sleep 심는 합성 chaos 코드 지양. 인프라 레벨 주입(docker stop / kubectl scale / 리소스 limit)을 우선하고, 코드 변경은 "계측 구멍 메우기"에만 쓴다. (근거: §11 주입 방식 결정 근거)
2. **정답 시그널이 관측 가능한지 먼저 확인** — 시그널이 안 잡히는 문항은 AI가 못 맞추는 게 아니라 문항이 성립 안 하는 것. 전제 작업(§10)이 그 조건이다.
3. **증상→원인 거리(hop)를 다양하게** — 1-hop(AU-3)부터 다중 서비스 복합(IN-1)까지 섞어야 변별력 있는 문항 세트가 된다.

## 0. 사전 조건

실행 전에 반드시 완료되어 있어야 하는 것.

- [ ] §10 사전 작업 Step 0 완료 — 완료 전에는 "레포 기준 원복" 수칙이 성립하지 않는다(레포본이 배포되면 서비스가 죽는 상태였음).
- [ ] ArgoCD auto-sync 동작 여부 확인 — 동작 중이면 kubectl patch류 주입(AU-1, AU-3)이 selfHeal에 수 초 내 원복되어 문항이 성립하지 않는다. 해당 문항 실행 블록에 sync 일시 해제가 포함되어 있다.
- [ ] 저트래픽 시간대인지 확인. 한 번에 한 문항만.
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
```

## 2. 실행 위치 · 측정 지점 · 원복 원칙

전제부터: **이 스택은 전부 k3s가 아니다.** 앱(content/chat/user)만 k3s 파드이고, MongoDB·Redis·Kafka는 별도 인프라 EC2의 docker 컨테이너다(configmap이 단일 인프라 사설 IP를 가리키는 게 증거). 그래서 `docker stop`은 kubectl이 아니라 그 인프라 노드의 순수 docker 명령이다. 명령마다 실행 위치가 갈린다.

### 2.1 어디서 무엇을 실행하나

| 명령 유형 | 예시 | 실행 위치 | 접근 방법 |
|---|---|---|---|
| kubectl (scale/patch/rollout/get) | `kubectl scale deploy/$AUTH_DEPLOY --replicas=0` | **master 노드** | kubeconfig가 master에 있음 |
| `$INFRA_SSH "docker ..."` | `$INFRA_SSH "docker stop $MONGO_CT"` | **master에서 타이핑** → SSH로 인프라 노드에 들어가 docker 실행 | 인프라 노드는 사설 IP라 VPC 안(master)에서만 닿음 |
| argocd app set | auto-sync 토글 | **master 노드** | `argocd login` 필요 |
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
| 로그 (fallback, JwtFilter, DLQ, Retry) | Grafana > Explore > Loki | 각 문항의 `{application="..."}` LogQL |
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
- **복구는 "명령 실행"이 아니라 "정상화 확인"까지다.** `kubectl get pods -n $NS`에서 Running/Ready, readiness UP(§2.2 터널), 그리고 대표 트리거(T1 또는 T4)가 200으로 복귀하는 것까지 본 뒤 다음 문항으로 넘어간다.
- ArgoCD selfHeal을 껐으면 원복 후 **반드시 다시 켠다**(§7.2).
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
BATTLE_ID=<선정값>; ITEM_ID=<선정값>
```

주의: 댓글 알림은 **아이템/배틀 작성자에게** 발송된다. 알림 도착까지 검증하려면 작성자 계정에 접근 가능한 배틀(내가 만든 테스트 배틀)을 쓴다.

### 3.3 baseline 채록 (모든 문항 공통, 주입 전 1회)

1. Grafana 홈 대시보드(Four Golden Signals) 정상 상태 스크린샷.
2. 정상 트레이스 1개: 아래 T1을 1회 실행하고, Tempo에서 `service.name = content-service`, 최근 5분으로 검색해 대표 traceId 기록.
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

```bash
# T1 — 댓글 작성. 40자 제한. 성공 시 200 + "코멘트가 등록되었습니다."
curl -s -X POST "$BASE/content/battles/$BATTLE_ID/items/$ITEM_ID/comments" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"content":"chaos-<ID>-'$(date +%H%M%S)'"}' | jq '.message'
```

## 5. 문항 카탈로그

| ID | 시나리오 (정답지) | 주입 | hop | 전제 (§10) |
|---|---|---|---|---|
| CH-1 | MongoDB 다운 → chat 컨슈머 실패 → 재시도 3회 → DLQ | `docker stop $MONGO_CT` | 2 | chat Step 0 배포 검증 |
| CH-2 | chat 다운 → 컨슈머 lag 누적 → 복구 후 알림 몰아서 도착 | `kubectl scale --replicas=0` | 2 | **lag 메트릭 — 미구현 시 보류** |
| AU-1 | auth CPU 기아 → 응답 지연 → content 캐시 미스 시 fallback | cpu limit 50m patch | 2~3 | (권장) auth JDBC 계측 |
| AU-2 | auth 완전 다운 → 로그인 502, content는 전부 익명 사용자 | `kubectl scale --replicas=0` | 2 | 없음 |
| AU-3 | JWT 시크릿 드리프트 → content 전 인증 API 401 (로그인은 성공) | Secret 변경 + restart | 1 | 없음 |
| IN-1 | Redis 다운 → 3개 서비스 동시 이상 | `docker stop $REDIS_CT` | 다중 | 스케줄러 @Observed 보강 |
| IN-2 | Kafka 다운 → 댓글 성공, 알림 조용히 유실 | `docker stop $KAFKA_CT` | 2 | 없음 |
| IN-3 | 커넥션 풀 고갈 → pending 적체 → 전면 지연 | k6 + 슬로우 쿼리 | 2 | Alert P0 룰 실구현 |
| IN-4 | Pod OOMKilled (보류) | heap 부하 | 2 | kube-state-metrics 활성화 전까지 보류 |

## 6. 문항별 실행 런북

모든 문항은 같은 형식이다: 주입 → 트리거 → 관측 → 판정 → 원복. 각 블록 상단 주석에 실행 셸(master / infra / 아무 셸)을 표기했다. 원복 명령은 주입 **전에** 셸 히스토리에 준비해둔다(§2.3).

### CH-1 — Mongo 다운 → 컨슈머 재시도 → DLQ

```bash
# 주입 — master 셸 (docker stop은 인프라 노드에서 실행됨)
date -u && $INFRA_SSH "docker stop $MONGO_CT"
# 트리거 — 아무 셸: T1 1건
# 원복 — master 셸
$INFRA_SSH "docker start $MONGO_CT" && date -u
```

관측 위치와 쿼리 (전부 Grafana):

- 댓글 API 응답: **200이어야 함** (발행은 AFTER_COMMIT 뒤, 실패는 컨슈머 쪽).
- Tempo: `service.name = chat-service`, `error = true`, 주입 시각 이후 — consume span 하위에서 Mongo insert 예외 → 재시도 → DLQ producer span까지 한 트레이스에 보이는지.
- Loki: `{application="chat-service"} |= "DLQ"` 및 `|= "Retry"` (라벨 값은 Grafana 라벨 브라우저에서 확인).
- 부수 증상: 채팅 메시지 전송/저장 실패 (Mongo 공유).

판정:

- [ ] 댓글 200 + 알림 미도착
- [ ] chat 트레이스에 FixedBackOff(1000ms × 3) 재시도 흔적 + DLQ 발행 span
- [ ] Mongo span에 error 태그
- [ ] 복구 후: DLQ 적재분 처리 방침 확인 — 재발행 수단이 없으면 그 사실을 answer.md 비고에 기록

정답지: "MongoDB 다운. 컨슈머는 3회 재시도 후 DLQ 적재 — 메시지 유실은 없음."

### CH-2 — 컨슈머 정지 → lag 누적 (전제 미충족 시 보류)

전제: chat에 컨슈머 lag 메트릭(`kafka_consumer_fetch_manager_records_lag*`)이 노출되어 있어야 한다. 없으면 정답 시그널이 관측 불가 — **§10의 5줄 작업 완료 전에는 실행하지 않는다.**

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

관측:

- PromQL: `websocket_active_users` — 0으로 급락 (주입 직후).
- PromQL: `kafka_consumer_fetch_manager_records_lag_max` — 누적 증가, 복구 후 해소 곡선.
- content 쪽 트레이스는 **깨끗해야 함** — "발행은 됐는데 소비가 없다"를 lag로 지목하는 것이 채점 포인트.
- 복구 후: 알림 몰아서 도착 확인.

판정: [ ] lag 누적·해소 곡선 채록 [ ] content 트레이스 무결 [ ] 복구 후 알림 일괄 도착

정답지: "chat 컨슈머 전멸로 lag 누적. 발행측 정상, 복구 시 밀린 알림 일괄 처리."

### AU-1 — auth CPU 기아 → 조용한 성능·데이터 품질 저하

```bash
# (ArgoCD auto-sync 동작 시) 주입 전 sync 일시 해제 — master 셸
argocd app set user-dev --sync-policy none        # 완료 후: --sync-policy automated
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
# 원복 후: argocd app set user-dev --sync-policy automated
```

관측:

- PromQL (로그인 P99): `histogram_quantile(0.99, sum by (le) (rate(http_server_requests_seconds_bucket{application="auth-service", uri="/login"}[5m])))` — 급등.
- Tempo: `service.name = content-service`, duration > 3s — `GET user-service` client span이 정확히 3s에서 잘리는 트레이스.
- Loki: `{application="content-service"} |= "fallback"` — ExternalUserApiClient의 fallback 로그.
- 사용자 가시 증상: 피드 작성자가 "사용자{id}"(익명)로 표시.
- **에러율(5xx)은 오르지 않아야 한다** — 이 문항의 변별 포인트.

판정: [ ] 로그인 P99 급등 [ ] 5xx 무변화 [ ] 3s로 잘린 client span [ ] fallback 로그·익명 표시

정답지: "auth CPU limit 과소로 응답 지연. content는 3s timeout + fallback으로 방어했으나 데이터 품질 저하."

### AU-2 — auth 완전 다운

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

관측·판정: 로그인 5xx(ingress 502) / content는 5xx 없음 / content client span이 **timeout(3s)이 아니라 connection refused로 즉시 실패** — AU-1과의 구별 자체가 채점 포인트.

정답지: "auth 전면 다운. content는 캐시 히트분 정상 + 미스분 익명 — fallback 설계 검증."

### AU-3 — JWT 시크릿 드리프트 (config drift, 전 사용자 영향 — 가장 마지막에, 가장 짧게)

```bash
# 백업 (평문 시크릿 포함 — 채록 후 즉시 삭제) — master 셸
kubectl -n $NS get secret content-secret -o yaml > /tmp/content-secret.backup.yaml
# (ArgoCD auto-sync 동작 시) content 앱 sync 일시 해제
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

관측:

- PromQL (4xx 비율): `sum(rate(http_server_requests_seconds_count{application="content-service", status="401"}[5m]))` — 인증 API 전반 급증.
- **401은 4xx라 span error 태그가 안 붙는다** — trace로는 못 찾는 문항. 메트릭(401 rate)과 `{application="content-service"} |= "JwtFilter"` 로그로 도달해야 한다. trace 의존도가 낮은 문항을 섞는 목적.

판정: [ ] 로그인 성공 + content 전 인증 API 401 [ ] 401 rate 급증 채록 [ ] JwtFilter 로그 채록

정답지: "content의 JWT_SECRET이 auth와 어긋남(config drift). 로그인은 되는데 아무것도 안 되는 상태."

### IN-1 — Redis 다운 (다중 서비스 복합, 최고 난도)

```bash
# 주입 — master 셸 (docker stop은 인프라 노드에서 실행됨)
date -u && $INFRA_SSH "docker stop $REDIS_CT"
# 트리거 — 아무 셸: T2 반복 + T1 1건. 스케줄러 증상은 트리거 없이 수 분 대기(핫스코어 주기 도래).
# 원복 — master 셸
$INFRA_SSH "docker start $REDIS_CT" && date -u
```

관측 (서비스별로 다르게 아픈 것을 단일 근원으로 수렴시키는 문항):

- content: user 캐시 실패 → auth 직행 호출 급증(Tempo에서 `GET user-service` client span 빈도), latency 상승. ShedLock 획득 실패 → 스케줄러 skip → **수 분 뒤 핫스코어 갱신 정체** (2차 지연 증상 — 스케줄러 @Observed 필요, §10).
- chat: 온라인 디바이스 조회 실패 → WS/FCM 발송 이상. Loki `{application="chat-service"} |= "Redis"`.
- auth: 이메일 인증·위치 검색 실패.
- 채록 전 확인: content `UserCacheStore`가 Redis 예외를 삼키고 API 직행하는지, 요청까지 깨지는지 — **실동작이 정답지의 일부.**

판정: [ ] 3개 서비스 증상 각각 채록 [ ] 스케줄러 skip 흔적 [ ] 단일 원인으로 수렴 가능한 시그널 경로 확인

정답지: "Redis 다운. 캐시·분산락·프레즌스·인증코드가 한꺼번에 무너지되 서비스별 증상이 다름."

### IN-2 — Kafka 다운 (조용한 유실 — 데이터 유실 동반, 허용 범위 먼저 결정)

```bash
# 주입 — master 셸 (docker stop은 인프라 노드에서 실행됨)
date -u && $INFRA_SSH "docker stop $KAFKA_CT"
# 트리거 — 아무 셸: T1 1건 (댓글은 200이어야 함)
# 원복 — master 셸
$INFRA_SSH "docker start $KAFKA_CT" && date -u
```

관측:

- 댓글 200 + 알림 영구 미도착 (재시도 없음 — 유실 복구 불가).
- Tempo: content producer span에 error 태그가 남는지 — **에러 경로에 error 태그가 남는지 검증과 직결.** `NotificationService.sendSafely`가 예외를 삼켜 **아무 데도 안 보인다**면 그 자체가 계측 구멍 발견 → 보강 후 재채록.
- chat: 채팅 발신 실패 (사용자 가시 증상).

판정: [ ] 댓글 200 [ ] producer span error 태그 유무 확인 [ ] 알림 유실 확인

정답지: "Kafka 다운. AFTER_COMMIT 발행이 실패하며 알림만 조용히 유실 — API는 전부 정상."

### IN-3 — 커넥션 풀 고갈

주입: 별도 주입 없음 — 슬로우 쿼리 문항(기존 문항 2)과 T5(k6 부하)를 조합하면 자연 발생.

```bash
# 아무 셸 (공개 ingress 대상)
k6 run k6/single-test.js -e BASE_URL=$BASE/content -e TOKEN=$TOKEN
```

관측:

- PromQL: `hikaricp_connections_pending{application="content-service"} > 0` — P0 알람 발화 여부까지 채록 (05-22 계획의 "실 발화 테스트"를 이 문항으로 소화).
- Tempo: connection acquire 대기가 JDBC span **앞단의 공백**으로 나타남 — span이 없는 구간을 읽는 능력이 채점 포인트.

판정: [ ] pending > 0 채록 [ ] P0 알람 발화 [ ] 트레이스 공백 구간 확인

정답지: "커넥션 풀 고갈. 슬로우 쿼리가 커넥션을 점유해 무관한 API까지 전면 지연 — DB가 아니라 커넥션 대기가 원인."

## 7. 실행 순서와 안전

### 7.1 권장 순서

증상→원인 거리(hop)가 짧고 주입이 단순한 것부터. 데이터 유실·전 사용자 영향 문항은 마지막에.

1. **AU-2** (auth scale 0) — 주입/원복이 가장 단순. 런북·토큰 점검 겸용.
2. **AU-1** (auth cpu 50m) — AU-2와 트레이스 구별(즉시 실패 vs 3s 잘림)을 연달아 채록.
3. **CH-1** (mongo 다운) — DLQ 경로.
4. **IN-2** (kafka 다운) — 조용한 유실. 유실 허용 범위 사전 결정.
5. **IN-1** (redis 다운) — 다중 서비스 복합, 최고 난도.
6. **IN-3** (커넥션 풀) — 부하 필요.
7. **AU-3** (JWT 드리프트) — 전 사용자 영향, **가장 마지막·가장 짧게**.
8. CH-2는 §10의 lag 메트릭 작업 완료 후에만.

### 7.2 안전 수칙 (문항 실행 전 반드시 확인)

- **이 클러스터가 곧 실서비스다.** 저트래픽 시간대에, 문항당 주입 시간을 최소화(수 분)하고, 복구 검증까지가 한 문항이다.
- **원복 명령은 주입 전에 준비.** 각 런북의 원복 줄을 먼저 셸에 붙여두고 주입한다(§2.3).
- **ArgoCD selfHeal 충돌 (Step 0에서 확인 필수).** ApplicationSet이 `automated + selfHeal`이면 `kubectl patch/scale` 주입을 수 초 내 되돌린다 → 문항 성립 안 함. patch·scale 계열(AU-1, AU-2, AU-3, CH-2) 주입 전 해당 Application의 auto-sync를 끄고, 원복 후 다시 켠다.

  ```bash
  # 주입 전 (해당 서비스-환경 Application) — master 셸
  argocd app set <svc>-<env> --sync-policy none
  # 원복 후
  argocd app set <svc>-<env> --sync-policy automated
  ```

  `<svc>-<env>`는 Step 0의 `kubectl get applications -n argocd` 결과로 확인. **주의: 로그인/인증 서비스의 실제 이름이 `user`인지 `auth`인지 레포 drift로 불확실**하다(§10 Step 0). live Application 이름 기준으로 잡는다.
- **kubectl 직접 변경은 매니페스트와 drift.** 채록 후 즉시 원복하고, `kubectl diff -k apps/<svc>/overlays/<env>`로 레포 기준과 일치하는지 확인한다. 단, 레포가 낡아 있으면(§10 Step 0) 이 확인이 무의미하니 그 정리가 선행되어야 한다.
- **데이터 유실 동반 문항**(IN-2 Kafka, CH-1 Mongo): 알림 유실/지연 허용 범위를 먼저 결정하고 진행.

## 8. 블라인드 채점 — RCA 품질을 어떻게 평가하나

"어떻게 평가하죠"에 대한 답. 문항을 만들어 놓고 끝이 아니라, **정답지를 가린 채 RCA를 돌리고 정해진 기준으로 점수를 매긴다.** 채점자가 시나리오를 이미 아는 상태로 하는 자기 채점은 무효 — 그래서 블라인드다.

절차:

1. **입력 고정** — 각 문항의 채록 창(주입~복구 UTC 구간)과 대상 서비스만 RCA에 준다. 정답지·주입 명령은 가린다.
2. **RCA 실행** — trace·Loki·metrics만으로 "원인 1줄 + 근거 경로"를 산출하게 한다(사람이든 AI든 동일 입력).
3. **채점** — 정답지와 대조해 아래 루브릭으로 점수화.

| 항목 | 배점 | 만점 기준 |
|---|---|---|
| 근본 원인 정확도 | 40 | 정답지의 원인 1줄과 일치 (예: "Redis 다운"이지 "지연 발생"이 아님) |
| 근거 시그널 경로 | 30 | 어떤 트레이스/메트릭/로그를 어떤 순서로 봤는지가 정답지의 근거 경로와 일치 |
| 오귀인 없음 | 20 | 무관한 컴포넌트를 원인으로 지목하지 않음 (예: AU-1을 DB 문제로 오진하지 않음) |
| 조치 타당성 | 10 | 제시한 복구/완화가 실제 원복과 방향이 맞음 |

채점의 변별력이 곧 문항 세트의 가치다:

- **hop이 다양해야 점수가 갈린다** — 1-hop(AU-3)은 대부분 맞히고, 다중 서비스 복합(IN-1)에서 근거 경로 점수가 벌어진다. 그 격차가 RCA 능력의 지표.
- **"에러율이 안 오르는 장애"(AU-1)에서 오귀인 항목이 살아난다** — 메트릭만 보는 RCA는 여기서 감점된다. 이런 함정 문항이 루브릭을 의미 있게 만든다.
- **동일 문항 반복 시 점수가 재현되는지**도 본다 — 재현 안 되면 문항이 불안정한 것(§9의 재현 가능성).

이 절이 "어떻게 평가하죠"에 대한 방어다: 문항은 실제 주입(§6)으로 진짜였고, 채점은 정답지를 가린 루브릭으로 객관화했다.

## 9. 채록 산출물

문항 하나 = 테스트케이스 하나. Given/When/Then으로 패키징한다.

```
docs/chaos/
  README.md              # 문항 카탈로그 + 결과 매트릭스 (아래)
  scripts/chaos.sh       # <문항ID> on|off — 주입·원복이 항상 쌍으로 존재
  scenarios/<ID>/
    runbook.md           # Given: 전제·baseline / When: 주입 + 트리거
    answer.md            # Then: 원인 1줄 + 근거 시그널 도달 경로
    evidence/            # baseline·증상 대시보드 캡처, 대표 traceId, Loki 쿼리, 알람 발화
```

결과 매트릭스가 포트폴리오의 얼굴이다:

| 문항 | 주입 | 사용자 증상 | 근거 시그널 | 탐지 수단 | RCA 점수 | 발견된 계측 구멍 → 보강 커밋 |
|---|---|---|---|---|---|---|

판단 근거(시니어 방어용):

1. **주입 충실도** — 앱 레벨 흉내(sleep/예외 assault)는 텔레메트리 모양이 실제 장애와 달라 무효. 인프라 레벨 주입만 쓴다(§11과 동일 논리). Then(기대 시그널)이 검증 대상인데 When이 가짜면 Then도 가짜다.
2. **재현 가능성이 테스트의 자격** — 주입·원복·전제·트리거가 전부 코드/문서로 존재해야 "실험"이 아니라 "테스트". 같은 문항을 다시 돌려 같은 시그널이 안 나오면 그게 회귀다.
3. **Then의 실패가 곧 성과** — 기대 시그널이 안 잡히면 문항 실패가 아니라 계측 구멍 발견이다(IN-2의 sendSafely 케이스). "장애를 주입해 관측성을 테스트하고, 실패한 assertion마다 보강 커밋이 나왔다"가 이 작업의 서사 — 시나리오 수보다 이 피드백 루프의 증거가 설득력이다.
4. **evidence 공개 주의** — 스크린샷에 사설 IP·내부 호스트명이 들어가지 않게 캡처 범위를 정한다(레포 공개 방침과 충돌 금지).

## 10. 사전 작업 (문항의 전제)

### Step 0 — 소스 오브 트루스 확인 (변경 없이, 모든 문항의 전제)

레포 base 이름(`content`/`chat`/`auth`/`user`)과 운영본 이름(`content-service` 등)이 다르고, 매니페스트 레포가 초기 스켈레톤에서 멈춰 있어 "레포 기준 원복"이 서비스를 죽일 수 있다. 아래로 live 사실을 먼저 확정한다(전부 master 셸).

```bash
kubectl get ns
kubectl get deploy,svc -A | grep -E 'content|chat|auth|user'   # 실제 이름·네임스페이스 → §1 변수에 반영
kubectl get pods -n argocd                                      # ArgoCD 동작 여부 → §7.2 selfHeal 판단
kubectl get applications -n argocd                              # Application 이름(<svc>-<env>) → auto-sync 토글 대상
```

- [ ] `NS`, `*_DEPLOY` 변수를 live 이름으로 교체
- [ ] 로그인/인증 서비스의 실제 배포명 확정(`user` vs `auth-service` drift 해소)
- [ ] ArgoCD가 selfHeal이면 §7.2의 auto-sync 토글을 patch/scale 문항에 반드시 적용
- [ ] (권장) live 스펙을 매니페스트 레포에 역반영해 drift를 닫은 뒤 채록 시작

### 문항별 사전 작업

| 대상 | 작업 | 없으면 성립 안 되는 문항 | 크기 |
|---|---|---|---|
| toy-chat | `ConsumerFactory`에 `MicrometerConsumerListener` 등록 → `kafka_consumer_fetch_manager_records_lag*` 노출 + Grafana lag 패널 | CH-2 (lag 시그널) | ~5줄 |
| toy-content | `FeedTrendingScheduler`·`ProductPopularityScheduler`에 `@Observed` | IN-1 스케줄러 증상 관측 | 2줄 |
| toy-user | `datasource-micrometer-spring-boot:1.1.1` 추가 (auth는 현재 JDBC span 전무) | AU-1/AU-2 auth 내부 원인 분해 | 의존성 1줄 |
| 인프라 | infra 노드 docker 컨테이너명·중지/기동 runbook 확인 (mongo/redis/kafka) | CH-1, IN-1, IN-2 주입 수단 | 문서 |
| 인프라 | Grafana Alert 룰 실구현 (P0 HikariCP → 5xx → P99) | IN-3 알람 발화 채록 | 대시보드 |

exporter(redis/kafka/mongodb-exporter)는 v1에서 불요 — 앱 쪽 시그널만으로 전 문항 정답 도달이 가능하다. 인프라 메트릭이 없어 성립 안 하는 건 CH-2(lag)뿐이고 앱 쪽 5줄로 해결한다.

## 11. 주입 방식 결정 근거 — 왜 앱 레벨 카오스를 안 쓰나

앱 레벨 옵션(프로필/플래그, chaos-monkey-spring-boot)은 배제한다. 주입 방식이 텔레메트리 모양을 바꾸기 때문. 예: CM4SB latency assault를 `ExternalUserApiClient`에 걸면 sleep이 메서드 앞에 끼므로 HTTP client span은 여전히 빠르고 server span에 원인 불명 공백만 생긴다 — AU-1 정답지(3s에서 잘리는 client span)와 다른 트레이스가 된다. RCA 문항은 정답 근거가 텔레메트리 모양 그 자체라, 흉내 낸 장애는 모양이 다르면 무효다. Kafka 다운을 예외 assault로 흉내 내도 connection refused 스택·타 서비스 동시 증상·복구 후 lag 해소 같은 진짜 장애의 지문이 안 생긴다.

대신 토글은 운영 계층에서 만든다 — `chaos.sh <문항ID> on|off`. 각 문항의 주입이 3~4줄 kubectl/docker이므로 래핑만 하면 되고, `off`(복구)가 항상 코드로 존재해 안전 수칙이 구조적으로 충족된다.
