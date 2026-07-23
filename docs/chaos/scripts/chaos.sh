#!/usr/bin/env bash
# chaos.sh — docs/chaos/RUNBOOK.md §3.3 사이클 실행기 (master 노드 전용)
#
# 사용법:
#   ./chaos.sh <문항ID> baseline   # ① 정상 측정 + ② 기록 — 게이트 실패(쿼리 빈 값) 시 exit 1 = 주입 금지
#   ./chaos.sh <문항ID> on         # ③ 주입 (argocd sync 해제 포함, 주입 시각 timeline.log 기록)
#   ./chaos.sh <문항ID> trigger    # ③ 트리거 (별도 루프가 있는 문항만: CH-2, AU-1)
#   ./chaos.sh <문항ID> symptom    # ④ 증상 관측 — baseline과 같은 함수를 재실행 (①=④ 동일 쿼리 보장)
#   ./chaos.sh <문항ID> off        # ⑤ 원복 + 복귀 확인 폴링
#   ./chaos.sh <문항ID> run        # ①~⑤ 전체 사이클 — 단계마다 확인 프롬프트, Ctrl-C 시 자동 원복(trap)
#
# 문항ID: CH-1 CH-2 AU-1 AU-2 AU-3 IN-1 IN-2 IN-3 AP-1 AP-2 AP-3
#   AP 계열: 주입 = 경계값 실요청 1건 (인프라 무접촉, 원복 없음 — RUNBOOK §6 AP 공통)
# 전제: 같은 디렉토리의 chaos.env (chaos.env.example 참고), jq, kubectl, curl, (argocd), (k6)
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

# t1 <태그> — 댓글 1건. HTTP코드+메시지 출력, 본문은 $EV 저장
t1() {
  local tag="$1" out="$EV/t1-$1.json" code
  code=$(curl -s -o "$out" -w '%{http_code}' \
    -X POST "$BASE/content/battles/$BATTLE_ID/items/$ITEM_ID/comments" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d '{"content":"chaos-'"$tag"'-'"$(date -u +%H%M%S)"'"}')
  echo "  T1 댓글: HTTP $code $(jq -r '.message // empty' "$out" 2>/dev/null)"
}

# fc <태그> <내용> — 피드 댓글 1건 (AP 계열). FC_CODE 설정, 본문은 $EV 저장. 내용은 jq로 안전 인코딩(이모지 포함)
fc() {
  local tag="$1" body out="$EV/fc-$tag.json"
  body=$(jq -cn --arg c "$2" '{content:$c}')
  FC_CODE=$(curl -s -o "$out" -w '%{http_code}' \
    -X POST "$BASE/content/feeds/${FEED_ID:-}/comments" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d "$body")
  echo "  피드 댓글($tag): HTTP $FC_CODE $(jq -r '.message // empty' "$out" 2>/dev/null)"
}

# t2 — 피드 스크롤. time_total 출력, 첫 페이지 본문 저장 (작성자 실명/익명 육안 확인용)
t2() {
  local t
  t=$(curl -s -o "$EV/t2-feed.json" -w '%{time_total}' "$BASE/content/feeds/scroll")
  echo "  T2 피드: time_total ${t}s → $EV/t2-feed.json"
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

argo_pause()  { if [ -n "${1:-}" ]; then argocd app set "$1" --sync-policy none      && log "argocd sync 해제: $1"; fi; }
argo_resume() { if [ -n "${1:-}" ]; then argocd app set "$1" --sync-policy automated && log "argocd sync 복원: $1"; fi; }

# ── CH-1: Mongo 다운 → 컨슈머 재시도 → DLQ ────────────────────────────────

measure_CH_1() {
  token && t1 "CH1-$PHASE"
  loki_count dlq   '{application="chat-service"} |= "DLQ"'
  loki_count retry '{application="chat-service"} |= "Retry"'
  tempo_search chat-error '{resource.service.name="chat-service" && status=error}'
  manual "알림 도착 여부(작성자 계정) — baseline: 도착 / symptom: 미도착"
  manual "baseline: 정상 트레이스(content→chat→Mongo) traceId 기록 / symptom: 재시도→DLQ span 모양 판독"
}
inject_CH_1() { infra "docker stop $MONGO_CT"; }
revert_CH_1() {
  infra "docker start $MONGO_CT"
  poll "mongo 컨테이너 Running" 12 5 infra "docker inspect -f '{{.State.Running}}' $MONGO_CT | grep -q true"
  manual "T1 1건으로 알림 도착 복귀 확인 + DLQ 적재분 처리 방침 answer.md에 기록"
}

# ── CH-2: 컨슈머 정지 → lag 누적 ─────────────────────────────────────────

measure_CH_2() {
  prom 1 lag_max         'kafka_consumer_fetch_manager_records_lag_max'
  prom 1 ws_active_users 'websocket_active_users'
  token && t1 "CH2-$PHASE"
  manual "알림 도착까지 소요 시간 기록 — 복구 후 '몰아서 도착'과 대조"
}
inject_CH_2() {
  argo_pause "$ARGO_APP_CHAT"
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
  argo_resume "$ARGO_APP_CHAT"
  manual "lag 해소 곡선 + 밀린 알림 일괄 도착 확인 (필요 시 symptom 재실행)"
}

# ── AU-1: auth CPU 기아 ──────────────────────────────────────────────────

measure_AU_1() {
  prom 0 login_p99 'histogram_quantile(0.99, sum by (le) (rate(http_server_requests_seconds_bucket{application="auth-service", uri="/login"}[5m])))'
  note "login_p99가 비면 로그인 몇 번 친 뒤 1~2분 후 재실행 (rate 계열은 트래픽 필요)"
  local t; t=$(curl -s -o /dev/null -w '%{time_total}' -X POST "$BASE/auth/login" \
    -H 'Content-Type: application/json' -d "$LOGIN_JSON")
  echo "  로그인 time_total: ${t}s"
  loki_count fallback '{application="content-service"} |= "fallback"'
  t2
  tempo_search content-slow '{resource.service.name="content-service" && duration > 3s}'
  manual "Tempo에서 GET user-service client span duration — baseline: 수십 ms / symptom: 3s 잘림"
  manual "t2-feed.json에서 작성자 실명(baseline) / 익명 '사용자{id}'(symptom) 육안 확인"
}
inject_AU_1() {
  argo_pause "$ARGO_APP_AUTH"
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
  argo_resume "$ARGO_APP_AUTH"
  poll "로그인 200 복귀" 24 5 login_ok
}

# ── AU-2: auth 완전 다운 ─────────────────────────────────────────────────

measure_AU_2() {
  local code; code=$(http_code -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "$LOGIN_JSON")
  echo "  로그인: HTTP $code (baseline 기대 200 / symptom 기대 502)"
  t2
  manual "symptom: content client span이 3s timeout이 아니라 connection refused 즉시 실패 — AU-1과 구별"
}
inject_AU_2() {
  argo_pause "$ARGO_APP_AUTH"
  kubectl -n "$NS" scale deploy/"$AUTH_DEPLOY" --replicas=0
}
revert_AU_2() {
  kubectl -n "$NS" scale deploy/"$AUTH_DEPLOY" --replicas=1
  kubectl -n "$NS" rollout status deploy/"$AUTH_DEPLOY"
  argo_resume "$ARGO_APP_AUTH"
  poll "로그인 200 복귀" 24 5 login_ok
}

# ── AU-3: JWT 시크릿 드리프트 ────────────────────────────────────────────

AU3_BK=/tmp/content-secret.backup.yaml

measure_AU_3() {
  token || return 1
  echo "  T4 인증 API: HTTP $(http_code "$BASE/content/feeds/following" -H "Authorization: Bearer $TOKEN") (baseline 기대 200 / symptom 기대 401)"
  prom 0 rate401 'sum(rate(http_server_requests_seconds_count{application="content-service", status="401"}[5m]))'
  loki_count jwtfilter '{application="content-service"} |= "JwtFilter"'
}
inject_AU_3() {
  kubectl -n "$NS" get secret "$CONTENT_SECRET" -o yaml > "$AU3_BK"
  grep -q "JWT_SECRET" "$AU3_BK" || { log "[중단] 백업에 JWT_SECRET 없음 — 시크릿 이름 확인"; rm -f "$AU3_BK"; return 1; }
  log "시크릿 백업: $AU3_BK (평문 포함 — 원복 시 자동 삭제)"
  argo_pause "$ARGO_APP_CONTENT"
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
  argo_resume "$ARGO_APP_CONTENT"
  poll "T4 200 복귀" 24 5 t4_ok
}

# ── IN-1: Redis 다운 (다중 서비스 복합) ──────────────────────────────────

measure_IN_1() {
  t2
  loki_count chat_redis    '{application="chat-service"} |= "Redis"'
  loki_count content_redis '{application="content-service"} |= "Redis"'
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
  tempo_search content '{resource.service.name="content-service"}'
  loki_count content_kafka '{application="content-service"} |= "Kafka"'
  manual "baseline: producer span이 트레이스 어디에 붙는지 기록 — symptom에서 '사라진 span'을 알아보는 기준"
  manual "symptom: producer span error 태그 유무 — 아무 데도 안 남으면 계측 구멍 발견(sendSafely)"
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
  loki_count datatoolong '{application="content-service"} |= "Data too long"'
  loki_count integrity   '{application="content-service"} |= "DataIntegrityViolationException"'
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

# ── AP-2: 대용량 업로드 → 실패 계층 판별 ─────────────────────────────────

AP2_FILE=/tmp/chaos-ap2.bin

measure_AP_2() {
  token || { GATE_FAIL=1; return; }
  head -c 1024 /dev/urandom > /tmp/chaos-ap2-small.bin
  local out="$EV/upload-small.json" code
  code=$(curl -s -o "$out" -w '%{http_code}' -X POST "$BASE/content/attachment-file/upload" \
    -H "Authorization: Bearer $TOKEN" -F "file=@/tmp/chaos-ap2-small.bin")
  echo "  1KB 업로드: HTTP $code → $out"
  rm -f /tmp/chaos-ap2-small.bin
  if [ "$PHASE" = baseline ] && [ "$code" != 200 ]; then
    echo "  [GATE] 정상 업로드가 HTTP $code — 잠복 버그(FileService 경로 구분자 하드코딩) 실증 가능성. 실버그로 기록 후 중단(§3.3)"; GATE_FAIL=1
  fi
  loki_count maxupload '{application="content-service"} |= "MaxUploadSizeExceededException"'
  prom 0 rate500 'sum(rate(http_server_requests_seconds_count{application="content-service", status="500"}[5m]))'
  manual "성공 업로드분(200)의 fileId 기록 — 서버 저장 파일이 정리 대상"
}
inject_AP_2() {
  token || return 1
  local mb="${AP2_MB:-2}" out="$EV/upload-big.txt" code
  log "${mb}MB 파일 생성 → 업로드 (AP2_MB로 조절: 통과하면 올려서 재시도)"
  dd if=/dev/zero of="$AP2_FILE" bs=1M count="$mb" status=none
  code=$(curl -s -o "$out" -w '%{http_code}' -X POST "$BASE/content/attachment-file/upload" \
    -H "Authorization: Bearer $TOKEN" -F "file=@$AP2_FILE")
  echo "  ${mb}MB 업로드: HTTP $code — 응답 본문: $out"
  rm -f "$AP2_FILE"
  note "판별: 413+HTML+앱시그널 없음=ingress / 500+JSON=multipart 미매핑 / 200=한도까지 통과(AP2_MB 올려 재시도)"
}
revert_AP_2() {
  rm -f "$AP2_FILE" /tmp/chaos-ap2-small.bin
  log "원복: 로컬 임시 파일 삭제 — 서버에 200으로 올라간 파일은 수동 정리(answer.md 기록)"
}

# ── AP-3: 이모지 댓글 → charset 불일치 (조건부) ──────────────────────────

EMOJI4=$'\U0001F600\U0001F389'   # 4바이트 문자 2개 (😀🎉)

measure_AP_3() {
  feed_id_set || return
  token || { GATE_FAIL=1; return; }
  fc "AP3-$PHASE-ascii" "chaos-AP3-ok-$(date -u +%H%M%S)"
  if [ "$PHASE" = baseline ] && [ "$FC_CODE" != 200 ]; then
    echo "  [GATE] ASCII 댓글이 HTTP $FC_CODE — 주입 금지(§3.3)"; GATE_FAIL=1
  fi
  loki_count charset '{application="content-service"} |= "Incorrect string value"'
  tempo_search content-error '{resource.service.name="content-service" && status=error}'
}
inject_AP_3() {
  token || return 1
  fc "AP3-emoji" "chaos-AP3-$(date -u +%H%M%S)-$EMOJI4"
  if [ "$FC_CODE" = 200 ]; then
    log "200 — 테이블 charset utf8mb4 확인: 문항 불성립. 그 자체를 answer.md에 기록하고 종료"
  else
    note "HTTP $FC_CODE — 증상 채록 진행 (기대 로그: Incorrect string value)"
  fi
}
revert_AP_3() {
  log "원복 없음 — 실패 시 롤백, 성공(불성립) 시 테스트 댓글만 남음"
  manual "테스트 댓글(chaos-AP3-*) 정리 여부 결정"
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
  CH-1|CH-2|AU-1|AU-2|AU-3|IN-1|IN-2|IN-3|AP-1|AP-2|AP-3) ;;
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
