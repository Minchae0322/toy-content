#!/usr/bin/env bash
# chaos.sh — docs/chaos/RUNBOOK.md §3.3 사이클 실행기 (master 노드 전용)
#
# 사용법:
#   ./chaos.sh <문항ID> baseline   # ① 정상 측정 + ② 기록 — 게이트 실패(쿼리 빈 값) 시 exit 1 = 주입 금지
#   ./chaos.sh <문항ID> on         # ③ 주입 (주입 시각 timeline.log 기록)
#   ./chaos.sh <문항ID> trigger    # ③ 트리거 (별도 루프가 있는 문항만: CH-2, AU-1)
#   ./chaos.sh <문항ID> symptom    # ④ 증상 관측 — baseline과 같은 함수를 재실행 (①=④ 동일 쿼리 보장)
#   ./chaos.sh <문항ID> off        # ⑤ 원복 + 복귀 확인 폴링
#   ./chaos.sh <문항ID> run        # ①~⑤ 전체 사이클 — 단계마다 확인 프롬프트, Ctrl-C 시 자동 원복(trap)
#
# 문항ID: CH-1 CH-2 AU-1 AU-2 AU-3 AU-4 IN-1 IN-2 IN-3 AP-1 AP-2 AP-3
#   AP 계열: 주입 = 경계값 실요청 1건 (인프라 무접촉, 원복 없음 — RUNBOOK §6 AP 공통)
# 전제: 같은 디렉토리의 chaos.env (chaos.env.example 참고), jq, kubectl, curl, (k6)
# 수동으로 남는 것: Tempo 트레이스 모양 판독, 알림 실제 도착 확인, 판정 체크박스, 블라인드 RCA(§8)

set -uo pipefail
cd "$(dirname "$0")"
[ -f chaos.env ] || { echo "chaos.env가 없습니다 — chaos.env.example을 복사해 채우세요"; exit 1; }
# shellcheck source=chaos.env.example
source chaos.env

ID="${1:-}"; CMD="${2:-}"
FN="${ID//-/_}"                          # CH-1 → CH_1 (함수명용)
RUN_TS="$(date -u +%Y%m%dT%H%M%SZ)"
SCEN_DIR="../scenarios/$ID"
TIMELINE="$SCEN_DIR/evidence/timeline.log"
EV="/tmp/chaos-ev"; mkdir -p "$EV"       # evdir 호출 전 기본값
PHASE=""
GATE_FAIL=0
TOKEN=""
LOGIN_JSON='{"email":"'"$EMAIL"'","password":"'"$PASSWORD"'"}'

# ── 공통 헬퍼 ──────────────────────────────────────────────────────────────

log()    { echo "[$(date -u +%H:%M:%SZ)] $*"; }
note()   { echo "        $*"; }
manual() { echo "  [수동] $*"; }
mark()   { mkdir -p "$(dirname "$TIMELINE")"; echo "$(date -u +%Y-%m-%dT%H:%M:%SZ) $*" >> "$TIMELINE"; }
evdir()  { EV="$SCEN_DIR/evidence/$1/$RUN_TS"; mkdir -p "$EV"; }
confirm(){ read -rp ">> $1 [Enter=진행 / Ctrl-C=중단] "; }
infra()  { $INFRA_SSH "$@"; }

# prom <gate:0|1> <이름> <promql> — 결과 저장 + 값 출력. gate=1이고 baseline에서 빈 값이면 게이트 실패.
prom() {
  local gate="$1" name="$2" q="$3" out val
  out="$EV/prom-$name.json"
  if ! curl -sf -u "$PROM_USER:$GRAFANA_TOKEN" -G "$PROM_URL/api/v1/query" \
       --data-urlencode "query=$q" -o "$out"; then
    echo "  [ERR] prom $name 쿼리 실패 (엔드포인트/토큰 확인)"; GATE_FAIL=1; return
  fi
  val=$(jq -r '[.data.result[].value[1]] | join(", ")' "$out")
  if [ -z "$val" ]; then
    if [ "$gate" = 1 ] && [ "$PHASE" = baseline ]; then
      echo "  [GATE] $name: 결과 없음 — 메트릭 미노출 또는 쿼리 오류 → 주입 금지(§3.3)"; GATE_FAIL=1
    else
      echo "  $name: (없음)"
    fi
  else
    echo "  $name: $val"
  fi
}

# loki_count <이름> <셀렉터+필터> — 최근 1h 건수 저장 + 출력
loki_count() {
  local name="$1" sel="$2" out val
  out="$EV/loki-$name.json"
  if ! curl -sf -u "$LOKI_USER:$GRAFANA_TOKEN" -G "$LOKI_URL/loki/api/v1/query" \
       --data-urlencode "query=sum(count_over_time($sel [1h]))" -o "$out"; then
    echo "  [ERR] loki $name 쿼리 실패"; return
  fi
  val=$(jq -r '.data.result[0].value[1] // "0"' "$out")
  echo "  $name(1h): ${val}건"
}

# tempo_search <이름> <TraceQL> — traceId 목록 저장 (모양 판독은 Grafana에서)
tempo_search() {
  local name="$1" q="$2" out n
  out="$EV/tempo-$name.json"
  if ! curl -sf -u "$TEMPO_USER:$GRAFANA_TOKEN" -G "$TEMPO_URL/api/search" \
       --data-urlencode "q=$q" --data-urlencode "limit=20" -o "$out"; then
    echo "  [ERR] tempo $name 검색 실패"; return
  fi
  n=$(jq -r '.traces | length' "$out" 2>/dev/null || echo "?")
  echo "  $name: trace ${n}건 → $out"
}

http_code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

token() {
  TOKEN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
    -d "$LOGIN_JSON" | jq -r '.accessToken // empty')
  [ -n "$TOKEN" ] || { echo "  [ERR] 로그인 실패 — 토큰 없음"; return 1; }
}

# json_or_gate <파일> <이름> — 응답이 JSON인지 검사.
# CDN 레이어가 API 404를 SPA index.html 200으로 마스킹하므로(2026-07-25 실증) http_code만으론 성공 판정 불가.
json_or_gate() {
  jq -e . "$1" >/dev/null 2>&1 && return 0
  echo "  [경고] $2 응답이 JSON 아님(SPA 폴백 의심 — 대상 ID 미실존?) — HTTP 코드 신뢰 불가"
  [ "$PHASE" = baseline ] && GATE_FAIL=1
  return 1
}

# t1 <태그> — 댓글 1건. HTTP코드+메시지 출력, 본문은 $EV 저장
t1() {
  local tag="$1" out="$EV/t1-$1.json" code
  code=$(curl -s -o "$out" -w '%{http_code}' \
    -X POST "$BASE/content/battles/$BATTLE_ID/items/$ITEM_ID/comments" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d '{"content":"chaos-'"$tag"'-'"$(date -u +%H%M%S)"'"}')
  echo "  T1 댓글: HTTP $code $(jq -r '.message // empty' "$out" 2>/dev/null)"
  json_or_gate "$out" "T1"
}

# t6 — 배틀 투표 1건 (쓰기 경로 커버 — T1 편중 방지). 재투표 정책상 4xx 가능, 코드·시간·메시지 그대로 채록
t6() {
  local out="$EV/t6-vote.json" meta
  meta=$(curl -s -o "$out" -w '%{http_code} %{time_total}s' \
    -X POST "$BASE/content/battles/$BATTLE_ID/items/vote" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d '{"votes":[{"itemId":'"$ITEM_ID"',"rank":1}]}')
  echo "  T6 투표: HTTP $meta $(jq -r '.message // empty' "$out" 2>/dev/null)"
  json_or_gate "$out" "T6"
}

# fc <태그> <내용> — 피드 댓글 1건 (AP 계열). FC_CODE 설정, 본문은 $EV 저장. 내용은 jq로 안전 인코딩(이모지 포함)
fc() {
  # local 한 줄 다중 할당은 인자를 전부 먼저 확장한다 — out에 $tag 쓰면 set -u에서 unbound. $1 직접 사용(t1과 동일 패턴)
  local tag="$1" body out="$EV/fc-$1.json"
  body=$(jq -cn --arg c "$2" '{content:$c}')
  FC_CODE=$(curl -s -o "$out" -w '%{http_code}' \
    -X POST "$BASE/content/feeds/${FEED_ID:-}/comments" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d "$body")
  echo "  피드 댓글($tag): HTTP $FC_CODE $(jq -r '.message // empty' "$out" 2>/dev/null)"
  json_or_gate "$out" "피드댓글"
}

# t2 — 피드 스크롤. time_total 출력, 첫 페이지 본문 저장 (작성자 실명/익명 육안 확인용)
t2() {
  local meta
  # ?size=10 필수 — 미지정 시 FeedService NPE로 500(2026-07-26 실버그, content 1e7df3f로 수정). HTTP코드도 검증(과거 t2는 코드 미확인으로 500을 조용히 통과시킴)
  meta=$(curl -s -o "$EV/t2-feed.json" -w '%{http_code} %{time_total}s' "$BASE/content/feeds/scroll?size=10")
  echo "  T2 피드: HTTP $meta → $EV/t2-feed.json"
  json_or_gate "$EV/t2-feed.json" "T2피드" || return
  # 작성자 익명 fallback 판별 — auth 다운+캐시미스 시 nickname이 "사용자N"(createFallbackUserInfo). AU-2/AU-4 증상
  local anon; anon=$(jq -r '[.data.content[]?.userInfo.nickname // empty | select(startswith("사용자"))] | length' "$EV/t2-feed.json" 2>/dev/null)
  [ -n "$anon" ] && [ "$anon" != "0" ] && note "익명 fallback 작성자 ${anon}명('사용자N') — auth 다운+캐시미스 신호"
}

# notif_inbox — 수신자(아이템 작성자) 계정 알림함 조회 (CH-1). RECIPIENT_EMAIL 미설정 시 수동 안내
notif_inbox() {
  if [ -z "${RECIPIENT_EMAIL:-}" ]; then
    manual "알림 도착 여부(수신자 계정) — baseline: 도착 / symptom: 미도착 (chaos.env RECIPIENT_* 설정 시 자동)"
    return
  fi
  local rt out="$EV/notif-inbox.json" code
  rt=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
    -d '{"email":"'"$RECIPIENT_EMAIL"'","password":"'"${RECIPIENT_PASSWORD:-}"'"}' | jq -r '.accessToken // empty')
  [ -n "$rt" ] || { echo "  [ERR] 수신자 로그인 실패 (RECIPIENT_* 확인)"; return; }
  code=$(curl -s -o "$out" -w '%{http_code}' "$BASE/chat/notifications?size=5" -H "Authorization: Bearer $rt")
  echo "  수신자 알림함: HTTP $code, 총 $(jq -r '.data.totalElements // "?"' "$out" 2>/dev/null)건 → $out"
  note "baseline: T1 알림 도착(+1) / symptom 주입 중: 조회 실패(5xx)도 그 자체가 증상 / 원복 후: 그 알림만 영구 부재"
}

login_ok()    { [ "$(http_code -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "$LOGIN_JSON")" = "200" ]; }
t4_ok()       { token >/dev/null 2>&1 && [ "$(http_code "$BASE/content/feeds/following" -H "Authorization: Bearer $TOKEN")" = "200" ]; }
pending_zero(){ curl -sf -u "$PROM_USER:$GRAFANA_TOKEN" -G "$PROM_URL/api/v1/query" \
                  --data-urlencode 'query=hikaricp_connections_pending{application="content-service"}' \
                | jq -e '[.data.result[].value[1]|tonumber] | add // 0 == 0' >/dev/null; }

# poll <설명> <횟수> <간격초> <명령...> — 복귀 확인 폴링
poll() {
  local desc="$1" n="$2" s="$3" i; shift 3
  for i in $(seq 1 "$n"); do
    if "$@" >/dev/null 2>&1; then log "복귀 확인: $desc"; return 0; fi
    sleep "$s"
  done
  log "[경고] 복귀 확인 실패: $desc — 수동 확인 필요"; return 1
}

# ── CH-1: Mongo 다운 → 컨슈머 재시도 → DLQ ────────────────────────────────

measure_CH_1() {
  token && t1 "CH1-$PHASE"
  prom 0 mongodb_up 'mongodb_up'
  loki_count publish    '{service_name="content-service"} |= "알림 발행"'   # 발행 자체가 일어났는지 — 트리거 공회전 검출
  loki_count dlq   '{service_name="chat-service"} |= "DLQ"'
  loki_count notif_fail '{service_name="chat-service"} |= "알림 처리 실패"'  # 컨슈머 catch 후 남는 실제 실패 로그 (Spring 재시도 로그는 "Retry" 문구를 안 남김)
  tempo_search chat-error '{resource.service.name="chat-service" && status=error}'
  notif_inbox
  manual "baseline: 정상 트레이스(content→chat→Mongo) traceId 기록 / symptom: consume span은 OK로 찍힘(예외 삼킴 함정) — 알림 처리 실패 로그의 traceId와 대조"
}
inject_CH_1() { infra "docker stop $MONGO_CT"; }
revert_CH_1() {
  infra "docker start $MONGO_CT"
  poll "mongo 컨테이너 Running" 12 5 infra "docker inspect -f '{{.State.Running}}' $MONGO_CT | grep -q true"
  manual "T1 1건으로 알림 도착 복귀 확인 + DLQ 적재분 처리 방침 answer.md에 기록"
}

# ── CH-2: 컨슈머 정지 → lag 누적 ─────────────────────────────────────────

measure_CH_2() {
  # lag는 kafka-exporter(브로커 측) 메트릭 — 컨슈머가 replicas=0이어도 계속 노출된다 (§10 앱 메트릭 전제를 이걸로 대체)
  prom 1 notif_lag       'sum(kafka_consumergroup_lag{consumergroup="notification-processors", topic="user.notifications"})'
  prom 1 ws_active_users 'websocket_active_users'
  token && t1 "CH2-$PHASE"
  manual "알림 도착까지 소요 시간 기록 — 복구 후 '몰아서 도착'과 대조"
}
inject_CH_2() {
  kubectl -n "$NS" scale deploy/"$CHAT_DEPLOY" --replicas=0
}
trigger_CH_2() {
  token || return 1
  log "댓글 30건 누적 (10초 간격, 약 5분)"
  local i; for i in $(seq 1 30); do t1 "CH2-load-$i" >/dev/null; sleep 10; done
  log "누적 완료"
}
revert_CH_2() {
  kubectl -n "$NS" scale deploy/"$CHAT_DEPLOY" --replicas=1
  kubectl -n "$NS" rollout status deploy/"$CHAT_DEPLOY"
  manual "lag 해소 곡선 + 밀린 알림 일괄 도착 확인 (필요 시 symptom 재실행)"
}

# ── AU-1: auth CPU 기아 ──────────────────────────────────────────────────

measure_AU_1() {
  prom 0 login_p99 'histogram_quantile(0.99, sum by (le) (rate(http_server_requests_seconds_bucket{application="auth-service", uri="/login"}[5m])))'
  note "login_p99가 비면 로그인 몇 번 친 뒤 1~2분 후 재실행 (rate 계열은 트래픽 필요)"
  local t; t=$(curl -s -o /dev/null -w '%{time_total}' -X POST "$BASE/auth/login" \
    -H 'Content-Type: application/json' -d "$LOGIN_JSON")
  echo "  로그인 time_total: ${t}s"
  loki_count fallback '{service_name="content-service"} |= "fallback"'
  t2
  tempo_search content-slow '{resource.service.name="content-service" && duration > 3s}'
  manual "Tempo에서 GET user-service client span duration — baseline: 수십 ms / symptom: 3s 잘림"
  manual "t2-feed.json에서 작성자 실명(baseline) / 익명 '사용자{id}'(symptom) 육안 확인"
}
inject_AU_1() {
  kubectl -n "$NS" patch deploy/"$AUTH_DEPLOY" --type=json \
    -p '[{"op":"replace","path":"/spec/template/spec/containers/0/resources/limits/cpu","value":"50m"}]'
  kubectl -n "$NS" rollout status deploy/"$AUTH_DEPLOY"
}
trigger_AU_1() {
  log "로그인 20회 (time_total 관찰)"
  local i; for i in $(seq 1 20); do
    curl -s -o /dev/null -w '%{time_total}s\n' -X POST "$BASE/auth/login" \
      -H 'Content-Type: application/json' -d "$LOGIN_JSON"
  done
  note "content fallback은 user 캐시 TTL 경과 후에야 보인다 — TTL 확인, 그동안 T2 주기 실행 권장"
}
revert_AU_1() {
  kubectl -n "$NS" patch deploy/"$AUTH_DEPLOY" --type=json \
    -p '[{"op":"replace","path":"/spec/template/spec/containers/0/resources/limits/cpu","value":"'"$AUTH_CPU_NORMAL"'"}]'
  kubectl -n "$NS" rollout status deploy/"$AUTH_DEPLOY"
  poll "로그인 200 복귀" 24 5 login_ok
}

# ── AU-2: auth 완전 다운 ─────────────────────────────────────────────────

measure_AU_2() {
  local code; code=$(http_code -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "$LOGIN_JSON")
  echo "  로그인(직접 auth): HTTP $code (baseline 200 / symptom 503 — ingress에 ready 엔드포인트 없음)"
  t2
  manual "핵심 판정: 직접 경로(login)만 죽고 content 피드는 200 유지(캐시)여야 정상 — content는 auth와 decoupling"
  manual "symptom: content client span이 3s timeout이 아니라 connection refused 즉시 실패 — AU-1과 구별. 캐시 만료 상황은 AU-4"
}
inject_AU_2() {
  kubectl -n "$NS" scale deploy/"$AUTH_DEPLOY" --replicas=0
}
revert_AU_2() {
  kubectl -n "$NS" scale deploy/"$AUTH_DEPLOY" --replicas=1
  kubectl -n "$NS" rollout status deploy/"$AUTH_DEPLOY"
  poll "로그인 200 복귀" 24 5 login_ok
}

# ── AU-3: JWT 시크릿 드리프트 ────────────────────────────────────────────

AU3_BK=/tmp/content-secret.backup.yaml

measure_AU_3() {
  token || return 1
  echo "  T4 인증 API: HTTP $(http_code "$BASE/content/feeds/following" -H "Authorization: Bearer $TOKEN") (baseline 기대 200 / symptom 기대 401)"
  prom 0 rate401 'sum(rate(http_server_requests_seconds_count{application="content-service", status="401"}[5m]))'
  loki_count jwtfilter '{service_name="content-service"} |= "JwtFilter"'
}
inject_AU_3() {
  kubectl -n "$NS" get secret "$CONTENT_SECRET" -o yaml > "$AU3_BK"
  grep -q "JWT_SECRET" "$AU3_BK" || { log "[중단] 백업에 JWT_SECRET 없음 — 시크릿 이름 확인"; rm -f "$AU3_BK"; return 1; }
  log "시크릿 백업: $AU3_BK (평문 포함 — 원복 시 자동 삭제)"
  kubectl -n "$NS" patch secret "$CONTENT_SECRET" \
    -p '{"stringData":{"JWT_SECRET":"chaos-au3-drift-value-not-a-real-secret-0000000000"}}'
  kubectl -n "$NS" rollout restart deploy/"$CONTENT_DEPLOY"
  kubectl -n "$NS" rollout status deploy/"$CONTENT_DEPLOY"
}
revert_AU_3() {
  [ -s "$AU3_BK" ] || { log "[중단] 백업 파일 없음 — 수동 복구 필요: $AU3_BK"; return 1; }
  kubectl -n "$NS" apply -f "$AU3_BK"
  kubectl -n "$NS" rollout restart deploy/"$CONTENT_DEPLOY"
  kubectl -n "$NS" rollout status deploy/"$CONTENT_DEPLOY"
  rm -f "$AU3_BK" && log "시크릿 백업 삭제"
  poll "T4 200 복귀" 24 5 t4_ok
}

# ── AU-4: auth 다운 + user 캐시 만료 (fallback 경로 실검증) ──────────────
# AU-2는 캐시 히트라 content 무영향(실명 유지). AU-4는 캐시 TTL(10분) 경과 후 채록 —
# content가 auth 직행 → 3s timeout → createFallbackUserInfo(익명 "사용자N")로 저하되는지,
# 아니면 무너지는지(500)를 본다. 주입은 AU-2와 동일(auth scale 0), 차이는 대기 시간과 판정.

measure_AU_4() {
  local code; code=$(http_code -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "$LOGIN_JSON")
  echo "  로그인(직접 auth): HTTP $code (baseline 200 / symptom 503)"
  t2   # ?size=10 — 작성자 실명(캐시 히트)/익명 '사용자N'(캐시 만료+auth 다운) 판별
  loki_count user_fallback '{service_name="content-service"} |~ "대체 사용자|fallback|Fallback"'
  prom 0 user_client_p99 'histogram_quantile(0.99, sum by (le) (rate(http_client_requests_seconds_bucket{application="content-service"}[5m])))'
  manual "핵심 판정: 캐시 만료(주입 10분+ 경과) 후 T2 작성자가 익명 '사용자N'이면 fallback 정상 / 500·에러면 fallback 붕괴(실버그)"
  manual "AU-2와 차이: AU-2=캐시 히트로 실명 유지(무영향), AU-4=캐시 만료로 fallback 경로 실검증. 3s timeout(ExternalUserApiClient) 후에야 fallback"
}
inject_AU_4() {
  kubectl -n "$NS" scale deploy/"$AUTH_DEPLOY" --replicas=0
  note "user 캐시 TTL 10분(UserCacheStore.DEFAULT_TTL) — symptom 채록은 주입 후 10분+ 대기 필수"
  note "10분 전에는 캐시 히트라 AU-2와 구별 안 됨. 그동안 T2를 주기적으로 쳐 캐시 만료 시점 전환을 관측 권장"
}
revert_AU_4() {
  kubectl -n "$NS" scale deploy/"$AUTH_DEPLOY" --replicas=1
  kubectl -n "$NS" rollout status deploy/"$AUTH_DEPLOY"
  poll "로그인 200 복귀" 24 5 login_ok
}

# ── IN-1: Redis 다운 (다중 서비스 복합) ──────────────────────────────────

measure_IN_1() {
  t2
  token && t6
  note "T6(쓰기 경로)가 Redis 다운에도 정상인지 자체가 채록 대상 — 아픈 경로/멀쩡한 경로의 대비가 변별"
  loki_count chat_redis    '{service_name="chat-service"} |= "Redis"'
  loki_count content_redis '{service_name="content-service"} |= "Redis"'
  tempo_search content '{resource.service.name="content-service"}'
  manual "GET user-service client span 빈도 — baseline: 낮음(캐시 히트) / symptom: 급증(직행)"
  manual "[게이트] 스케줄러 @Observed span이 주기대로 찍히는지 — 안 보이면 §10 전제 미충족, 주입 금지"
  manual "최근 핫스코어 갱신 시각 기록 — symptom의 '갱신 정체'와 대조"
}
inject_IN_1() { infra "docker stop $REDIS_CT"; }
revert_IN_1() {
  infra "docker start $REDIS_CT"
  poll "redis 컨테이너 Running" 12 5 infra "docker inspect -f '{{.State.Running}}' $REDIS_CT | grep -q true"
  manual "캐시 히트·스케줄러 주기 baseline 상태 복귀 확인"
}

# ── IN-2: Kafka 다운 (조용한 유실) ───────────────────────────────────────

measure_IN_2() {
  token && t1 "IN2-$PHASE"
  prom 0 kafka_brokers 'kafka_brokers'    # CH-1의 mongodb_up에 대응 — kafka-exporter가 브로커 다운 시 0 또는 부재
  tempo_search content '{resource.service.name="content-service"}'
  loki_count content_kafka '{service_name="content-service"} |= "Kafka"'
  loki_count pub_ok   '{service_name="content-service"} |= "알림 발행 성공"'   # KafkaNotificationProducer whenComplete 성공 로그
  loki_count pub_fail '{service_name="content-service"} |= "알림 발행 실패"'   # 실측(회차 1): send()가 max.block.ms(60s) 동기 블로킹 후 스로우 → T1+60s에 리스너 catch가 찍음
  loki_count chat_disconnect '{service_name="chat-service"} |~ "Broker may not be available|Connection to node"'   # 실측(회차 1): 항상 0 — chat은 org.apache.kafka=ERROR로 억제(그 0이 곧 발견)
  notif_inbox
  manual "baseline: producer span이 트레이스 어디에 붙는지 기록 — symptom에서 '사라진 span'을 알아보는 기준"
  manual "symptom: 트리거 트레이스에 notification-publish ~60,060ms error span + chat span 전면 부재 확인 (회차 1 실측 — answer.md 도달 경로)"
}
inject_IN_2() { infra "docker stop $KAFKA_CT"; }
revert_IN_2() {
  infra "docker start $KAFKA_CT"
  poll "kafka 컨테이너 Running" 12 5 infra "docker inspect -f '{{.State.Running}}' $KAFKA_CT | grep -q true"
  manual "T1 1건으로 알림 도착 복귀 확인 (주입 중 유실분은 복구 불가 — answer.md에 기록)"
}

# ── IN-3: 커넥션 풀 고갈 ─────────────────────────────────────────────────

measure_IN_3() {
  prom 1 hikari_pending 'hikaricp_connections_pending{application="content-service"}'
  prom 0 content_p99 'histogram_quantile(0.99, sum by (le) (rate(http_server_requests_seconds_bucket{application="content-service"}[5m])))'
  token && t6
  note "T6 응답 시간 — 부하와 무관한 쓰기 경로까지 지연되는지('전면'의 실증)"
  manual "Grafana Alerting에서 P0 룰 존재 + 평시 미발화 확인 (§10 전제)"
  manual "symptom: JDBC span 앞단의 공백 구간(트레이스에 없는 대기) 판독"
}
inject_IN_3() {
  log "별도 주입 없음 — k6 부하 실행 (블로킹, 종료가 곧 부하 해제)"
  token || return 1
  k6 run "$K6_SCRIPT" -e BASE_URL="$BASE/content" -e TOKEN="$TOKEN"
}
revert_IN_3() {
  log "원복 없음 — 부하 종료 후 pending 해소 대기"
  poll "hikaricp pending 0 복귀" 24 5 pending_zero
}

# ── AP 계열 공통: 주입 = 경계값 실요청, 원복 없음 (RUNBOOK §6 AP 공통) ────

feed_id_set() {
  if [ -z "${FEED_ID:-}" ]; then
    echo "  [GATE] FEED_ID 미설정 (chaos.env) — §3.2에서 선정 후 기입"; GATE_FAIL=1; return 1
  fi
}

# ── AP-1: 댓글 201자 → varchar(200) 위반 → 500 ──────────────────────────

measure_AP_1() {
  feed_id_set || return
  token || { GATE_FAIL=1; return; }
  fc "AP1-$PHASE-ok" "chaos-AP1-ok-$(date -u +%H%M%S)"
  if [ "$PHASE" = baseline ] && [ "$FC_CODE" != 200 ]; then
    echo "  [GATE] 정상 댓글이 HTTP $FC_CODE — FEED_ID/토큰/서비스부터 규명 → 주입 금지(§3.3)"; GATE_FAIL=1
  fi
  loki_count datatoolong '{service_name="content-service"} |= "Data too long"'
  loki_count integrity   '{service_name="content-service"} |= "DataIntegrityViolationException"'
  prom 0 rate500 'sum(rate(http_server_requests_seconds_count{application="content-service", status="500"}[5m]))'
  tempo_search content-error '{resource.service.name="content-service" && status=error}'
  manual "symptom: INSERT JDBC span error 태그 — Tempo에서 모양 판독"
  manual "symptom: 정상 댓글이 여전히 200인 것 = 전면 장애 아님 — 판정의 일부"
}
inject_AP_1() {
  token || return 1
  local long; long=$(printf 'a%.0s' $(seq 1 250))
  fc "AP1-over250" "$long"
  note "기대: 500 (Data too long) — 200이면 검증이 이미 보강된 것, 문항 불성립으로 answer.md에 기록"
}
revert_AP_1() {
  log "원복 없음 — 실패 INSERT는 롤백되어 상태 무변화"
  token && fc "AP1-recover" "chaos-AP1-recover-$(date -u +%H%M%S)"
  manual "테스트 댓글(chaos-AP1-*) 정리 여부 결정"
}

# ── AP-2: 팔로우 목록 size 미기본값 → size+1 언박싱 NPE → 500 ──────────────

measure_AP_2() {
  token || { GATE_FAIL=1; return; }
  # 정상 요청: size 명시 → 200 기대 (게이트). size 생략만 500이어야 문항 성립
  local out="$EV/following-size20.json" code
  code=$(curl -s -o "$out" -w '%{http_code}' \
    "$BASE/auth/user/1/following?size=20" -H "Authorization: Bearer $TOKEN")
  echo "  following?size=20: HTTP $code → $out"
  if [ "$PHASE" = baseline ] && [ "$code" != 200 ]; then
    echo "  [GATE] size 명시 정상 요청이 HTTP $code — 전제(사용자/토큰/서비스) 규명 필요, 주입 금지(§3.3)"; GATE_FAIL=1
  fi
  loki_count follow-npe '{service_name="auth-service"} |= "NullPointerException"'
  prom 0 rate500 'sum(rate(http_server_requests_seconds_count{application="auth-service", status="500"}[5m]))'
  tempo_search auth-error '{resource.service.name="auth-service" && status=error}'
}
inject_AP_2() {
  token || return 1
  log "size 파라미터 생략 GET 2건 — 선택 파라미터 미기본값 → limit()의 size+1 언박싱 NPE 발화"
  local ep out code
  for ep in following followers; do
    out="$EV/follow-$ep.json"
    code=$(curl -s -o "$out" -w '%{http_code}' \
      "$BASE/auth/user/1/$ep" -H "Authorization: Bearer $TOKEN")
    echo "  GET user/1/$ep (size 생략): HTTP $code — 기대 500, 본문: $out"
  done
  note "판별: 500+NPE(FollowCondition.limit)+DB span 없음=코드 언박싱 / size=20이면 200 공존=전면장애 아님 / DB 오귀인 금지"
}
revert_AP_2() {
  log "원복 없음 — GET 실패는 상태 무변화. size=20 정상 200 재확인만."
}

# ── AP-3: 중복 해시태그 → uk_feed_hashtag 유니크 위반 → 500 ──────────────
#   (구 이모지 charset 문항 대체 — 2026-07-28. RUNBOOK §6 AP-3 참조)

# feed_create <태그> <hashtags_json> — 첨부 업로드 → 카테고리 → 피드 생성 1건. CF_CODE 설정, 본문 $EV 저장.
#   FEED_ID 불필요(피드를 새로 만든다). hashtags_json은 JSON 배열 문자열: 게이트 '["chaosok"]' / 주입 '["coffee","COFFEE"]'.
#   업로드→피드 순서·필드는 rca-agent scripts/api-write-flow.sh와 동일.
FEED_IMG=""
feed_create() {
  local tag="$1" hashtags="$2" out="$EV/cf-$1.json" up="$EV/cf-upload-$1.json" cat="$EV/cf-categories.json"
  local fid stored origin sub body
  # 0) 1x1 PNG 준비 (최초 1회)
  if [ -z "$FEED_IMG" ]; then
    FEED_IMG="$EV/ap3-1x1.png"
    base64 -d > "$FEED_IMG" <<<'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='
  fi
  # 1) 첨부 업로드
  curl -s -o "$up" -X POST "$BASE/auth/files/upload" \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@$FEED_IMG" -F "fileExplain=chaos-AP3-$tag" >/dev/null
  json_or_gate "$up" "업로드" || { CF_CODE=000; return; }
  fid=$(jq -r '.data.id // .id // empty' "$up")
  stored=$(jq -r '.data.fileUrl // .fileUrl // empty' "$up")
  origin=$(jq -r '.data.orgFileNm // .orgFileNm // "chaos.png"' "$up")
  [ -n "$fid" ] && [ -n "$stored" ] || { echo "  [GATE] 업로드 응답에 fileId/fileUrl 없음 → 주입 금지"; [ "$PHASE" = baseline ] && GATE_FAIL=1; CF_CODE=000; return; }
  # 2) 서브카테고리 (실패 시 SUB_ID 또는 1)
  curl -s -o "$cat" "$BASE/content/categories" -H "Authorization: Bearer $TOKEN" >/dev/null
  sub=$(jq -r '[.. | objects | .categoryId?] | map(select(. != null)) | .[0] // empty' "$cat" 2>/dev/null)
  [ -n "$sub" ] || sub="${SUB_ID:-1}"
  # 3) 피드 생성 (hashtags만 문항 변수)
  body=$(jq -cn --argjson uid "${USER_ID:-1}" --argjson sub "$sub" \
    --argjson fid "$fid" --arg stored "$stored" --arg origin "$origin" \
    --argjson hashtags "$hashtags" --arg mark "chaos-AP3-$tag-$(date -u +%H%M%S)" '
    { userId:$uid, productId:null, productNameCustom:$mark, subCategoryId:$sub,
      review:$mark, buyPlace:"chaos", buyPrice:null, price:null, evaluation:"GOOD",
      thumbnailAttachmentInfo:{fileId:$fid, storedPath:$stored, originName:$origin},
      attachmentFileInfos:[{fileId:$fid, storedPath:$stored, originName:$origin}],
      hashtags:$hashtags }')
  CF_CODE=$(curl -s -o "$out" -w '%{http_code}' \
    -X POST "$BASE/content/feeds" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$body")
  echo "  피드 생성($tag, hashtags=$hashtags): HTTP $CF_CODE $(jq -r '.message // empty' "$out" 2>/dev/null)"
}

measure_AP_3() {
  token || { GATE_FAIL=1; return; }
  # 게이트: 중복 없는 해시태그로 정상 피드 생성 → 200이어야 성립 (업로드·카테고리·토큰 규명 겸용)
  feed_create "$PHASE-ok" '["chaosok"]'
  if [ "$PHASE" = baseline ] && [ "$CF_CODE" != 200 ]; then
    echo "  [GATE] 정상 피드 생성이 HTTP $CF_CODE — 업로드/카테고리/토큰부터 규명 → 주입 금지(§3.3)"; GATE_FAIL=1
  fi
  loki_count duplicate  '{service_name="content-service"} |= "Duplicate entry"'
  loki_count integrity  '{service_name="content-service"} |= "DataIntegrityViolationException"'
  prom 0 rate500 'sum(rate(http_server_requests_seconds_count{application="content-service", status="500"}[5m]))'
  tempo_search content-error '{resource.service.name="content-service" && status=error}'
  manual "symptom: 피드 생성 INSERT span error 태그 — Duplicate entry '<feedId>-<hashtagId>' for key 'tb_feed_hashtags.uk_feed_hashtag' (Tempo 판독)"
}
inject_AP_3() {
  token || return 1
  log "정규화 충돌 해시태그(coffee/COFFEE) 피드 생성 1건 — dedup 부재로 uk_feed_hashtag 유니크 위반 발화"
  feed_create "dup" '["coffee","COFFEE"]'
  if [ "$CF_CODE" = 200 ]; then
    log "200 — dedup이 이미 추가됐거나 제약 소멸: 문항 불성립. answer.md에 기록하고 종료"
  else
    note "HTTP $CF_CODE — 증상 채록 진행 (기대: Duplicate entry '<feedId>-<hashtagId>' for key 'tb_feed_hashtags.uk_feed_hashtag')"
  fi
}
revert_AP_3() {
  log "원복 없음 — 실패 INSERT는 롤백. 게이트의 정상 피드(chaos-AP3-*-ok)만 남음"
  manual "테스트 피드(chaos-AP3-*) 정리 여부 결정"
}

# ── run: ①~⑤ 전체 사이클 ────────────────────────────────────────────────

do_run() {
  PHASE=baseline; evdir baseline
  log "① 정상 측정 (게이트) → $EV"
  "measure_$FN"
  if [ "$GATE_FAIL" = 1 ]; then
    log "게이트 실패 — 주입 금지(§3.3). 전제(§10)나 쿼리부터 해결."; exit 1
  fi
  echo
  confirm "② baseline 기록 완료. ③ 주입을 시작할까요? (중단 시 자동 원복 trap 설정됨)"
  trap 'echo; log "[중단 감지] 자동 원복 실행"; "revert_'"$FN"'"; mark "ABORT-REVERT $ID"; exit 1' INT TERM
  mark "INJECT $ID"
  log "③ 주입"
  "inject_$FN"
  if declare -F "trigger_$FN" >/dev/null; then
    log "③ 트리거"
    "trigger_$FN"
  else
    note "별도 트리거 없음 — symptom 채록에 T1/T2가 포함됨"
  fi
  echo
  confirm "④ 증상 채록 — 시그널 적재(스크레이프·로그 전송 ~1분) 대기 후 Enter"
  PHASE=symptom; evdir symptom
  log "④ 증상 관측 (①과 같은 쿼리) → $EV"
  "measure_$FN"
  note "시그널이 늦으면 원복 전에 ./chaos.sh $ID symptom 재실행 가능 (새 타임스탬프 폴더)"
  echo
  confirm "⑤ 원복할까요?"
  log "⑤ 원복 + 복귀 확인"
  "revert_$FN"
  trap - INT TERM
  mark "REVERT $ID"
  echo
  log "완료 — 채록 창은 $TIMELINE 참조 (§8 블라인드 채점 입력)"
  note "남은 수동 작업: 판정 체크박스(§6), Tempo 모양 판독, 스크린샷 보강, answer.md"
}

# ── 디스패처 ─────────────────────────────────────────────────────────────

usage() { sed -n '2,14p' "$0"; }

case "$ID" in
  CH-1|CH-2|AU-1|AU-2|AU-3|AU-4|IN-1|IN-2|IN-3|AP-1|AP-2|AP-3) ;;
  *) usage; exit 1 ;;
esac

case "$CMD" in
  baseline)
    PHASE=baseline; evdir baseline
    log "① 정상 측정 → $EV"
    "measure_$FN"
    if [ "$GATE_FAIL" = 1 ]; then log "게이트 실패 — 주입 금지(§3.3)"; exit 1; fi
    log "② baseline 저장 완료: $EV"
    ;;
  on)
    mark "INJECT $ID"
    log "③ 주입: $ID (시각은 $TIMELINE)"
    "inject_$FN"
    ;;
  trigger)
    if declare -F "trigger_$FN" >/dev/null; then "trigger_$FN"
    else echo "이 문항은 별도 트리거 없음 — symptom 채록에 T1/T2 포함"; fi
    ;;
  symptom)
    PHASE=symptom; evdir symptom
    log "④ 증상 관측 (baseline과 같은 쿼리) → $EV"
    "measure_$FN"
    log "symptom 저장 완료: $EV"
    ;;
  off)
    log "⑤ 원복: $ID"
    "revert_$FN"
    mark "REVERT $ID"
    ;;
  run) do_run ;;
  *) usage; exit 1 ;;
esac
