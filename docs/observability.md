# Observability — 메트릭 / 트레이싱 / 로깅

Grafana Cloud Free(메트릭 10k 시리즈, 로그·트레이스 각 50GB/월, retention 14일) 위에 K3s + Alloy 1개로 통합 수집하는 구조. Spring Boot 측 인프라(Actuator + Micrometer + Brave/Zipkin)를 표준화해두고, 트래픽 성장 시 자체 호스팅 LGTM(VictoriaMetrics + Hybrid Pull/Push)으로 마이그레이션할 수 있도록 앱 코드가 모니터링 백엔드에 결합되지 않게 설계한다.

이 문서는 **앱 레벨에서 일어난 결정과 변경**을 누적해서 기록한다. Helm/Alloy/대시보드 설치는 별도 진행.

---

## 2026-05-20 추가 — SRE Four Golden Signals 대시보드 구성

### 무엇을 했나

Grafana Cloud에 `$application` 변수(template variable, multi-value) 기반 대시보드 1개 신규. 4개 패널 모두 `application=~"$application"` 필터 + `$__rate_interval` 사용 → 시간 범위에 따라 자동으로 1m/5m/15m 등으로 적응.

| 패널 | PromQL 핵심 | 단위/설정 |
|---|---|---|
| **Traffic** — Request Rate | `sum(rate(http_server_requests_seconds_count{application=~"$application"}[$__rate_interval])) by (application)` | `reqps`, Min=0 |
| **Errors** — 5xx % | `sum(rate(...{status=~"5.."}[...])) / sum(rate(...[...])) * 100` (by application) | `percent (0-100)`, Min=0 / Max=100, Thresholds: 1% yellow / 5% red, **As filled regions** |
| **Latency** — P50/P95/P99 | `histogram_quantile(0.50/0.95/0.99, sum(rate(http_server_requests_seconds_bucket{...}[...])) by (le, application))` × 3 쿼리 | `seconds`, **Log scale (base 2)** — P99 튀어도 P50과 한 화면 |
| **Saturation** — Tomcat threads / CPU | (별도 패널) | — |

모든 패널 Legend는 `{{application}}` custom 포맷.

### 핵심 의사결정

- **Four Golden Signals 기준** — Google SRE 정의(Latency / Traffic / Errors / Saturation). "어떻게 모니터링하시나요?" 면접 질문에 단일 프레임워크로 답 가능. 임의로 패널 나열하면 누락/중복 생기지만 4 시그널은 빠짐없고 겹침없다.
- **`$__rate_interval` 사용 (고정 `5m` 아님)** — Grafana가 dashboard time range + scrape interval로 자동 계산. zoom-in 시 `1m`, zoom-out 시 더 큰 윈도우. 고정값이면 짧은 구간에서 빈 그래프 또는 긴 구간에서 너무 노이지.
- **Error Rate threshold = 1% / 5%** — 1%는 "주의 깊게 봐야 함", 5%는 "사용자 피해 발생 중" 경험적 기준. Filled region으로 그려서 한 화면에 위험도 즉시 시각화.
- **Latency Log scale** — P99가 P50보다 10~100배 튀는 게 정상 분포. Linear scale이면 P50/P95가 바닥에 깔려서 안 보임. Log scale이 SRE 대시보드 표준.
- **`application` 라벨 기준 집계** — `pod`나 `instance`로 by 하면 시리즈 폭발(+ 면접에서 "deployment 단위 SLO"라는 답이 더 깔끔). 카디널리티 관리는 [[2026-05-17]] MeterFilter 결정의 연장선.

### 검증 방법

- chat-service: 약 0.15 RPS (health check 트래픽 추정), content-service: 0.005 RPS 미만(idle). 둘 다 그래프에 표시되어 `$application` multi-select 정상 동작 확인.
- Error Rate "No data" — 5xx가 한 건도 없으면 정상. Explore에서 `http_server_requests_seconds_count{status=~"5.."}` 직접 조회로 데이터 유무 사전 확인.
- Latency: 트래픽 적을 때 P50/P95/P99가 거의 같은 선으로 겹쳐 보이는 게 정상 — 분포가 좁아서. 부하 들어가면 분리됨.

### 핵심 사전 조건

- `http_server_requests_seconds_bucket` (histogram) 메트릭이 있어야 P95/P99 계산 가능. `application.yml`의 `management.metrics.distribution.percentiles-histogram.http.server.requests=true`가 [[2026-05-17]] 표준화 때 이미 켜져 있음.

### 다음 단계

- [ ] **Critical 알림 4개** — 메트릭은 다 있으므로 Grafana Alerting 규칙만 추가: 5xx 비율 > 5% (5m) / OOMKilled / Kafka consumer lag / p99 SLO 위반
- [ ] **비즈니스 메트릭** — `yogurtte.feed.created`, `yogurtte.battle.vote` 카운터 추가. 기술 메트릭과 별개 패널로 묶어 "Business KPI" 행 신설
- [ ] **k6 부하 → N+1 발견 → `@EntityGraph` 개선** 사이클 (면접용 임팩트 사례)

---

## 2026-05-20 — Loki ↔ Tempo 양방향 점프 검증 완료

### 무엇을 했나

| 영역 | 결과 |
|---|---|
| Tempo에서 Trace ID 검색 | `6a0d3fd0...` trace 정상 표시 |
| Trace → Logs 자동 점프 | trace span 클릭 시 우측 Loki 패널 자동 split view로 열림 |
| LogQL 라벨 변환 | `service.name` (OTel attr) → `service_name` (Loki 라벨)로 자동 정규화 — Grafana data source가 알아서 처리 |
| Trace ID 필터링 | `{service_name="content-service"}` + `trace_id="6a0d3fd0..."` 조합으로 같은 시간대 다른 trace 섞이지 않음 |
| 로그 결과 | 14:00:00 시점 `BattleHotScoreScheduler` 로그 2건 정확히 매칭 |
| Logs volume 그래프 | trace 발생 시점에 정확히 스파이크 |
| Loki → Tempo | 로그의 `View Trace` 링크 → trace 트리 정상 점프 (derivedFields 정규식 동작) |

Grafana가 자동 생성한 LogQL:

```logql
{service_name="content-service"}
| label_format log_line_contains_trace_id=`{{ contains "6a0d3fd0..." __line__ }}`
| log_line_contains_trace_id="true" or trace_id="6a0d3fd0..."
```

### 핵심 의사결정

- **"Logs for this span" 별도 버튼 없음 = 정상** — 이전 검증 노트에서 버튼이 안 보인다고 적어뒀는데, Grafana는 trace 검색 시 우측 패널에 자동으로 관련 로그를 띄우는 split view가 기본 UX. 버튼은 없어도 점프는 되고 있는 상태였다. UI 요소 부재를 곧장 "기능 누락"으로 단정하지 말 것.
- **두 방식 OR로 둠** — 자동 생성된 LogQL이 (1) 로그 라인에 `traceId=...` 문자열 포함 여부 (2) `trace_id` 라벨 일치, 둘 중 하나만 맞으면 표시. 우리 케이스는 MDC 패턴으로 로그 본문에 박혀 있는 (1) 경로지만, OTel 자동 계측 환경(라벨로 주입)이 섞여도 누락 없이 동작. 굳이 한쪽으로 통일하지 않음.
- **`trace_id`는 Loki 라벨 아닌 라인 내 값** — 이전 결정([[2026-05-18]] cardinality 항목) 그대로 유지. 검색은 line filter로 충분히 빠르고 50GB/월 한도를 지킴.

### 검증 방법

1. content-service에 임의 요청 흘려서 로그/trace 생성
2. Grafana Explore → Tempo → Trace ID로 검색
3. 좌측 trace 트리, 우측 Loki 로그 패널 동시 표시 확인
4. 같은 traceId의 로그가 시간순으로 정렬되어 trace span 시작/종료 시점과 일치하는지

### 다음 단계

- [ ] Service Graph 확인 (auth-service ↔ content-service, 24h 데이터 축적 후)
- [ ] DB/Redis span은 보류 — 필요해질 때 OTel agent / datasource-proxy 별도 판단 ([[2026-05-19]] 결정 그대로)
- [ ] 면접 자료용 스크린샷 보관: trace + 로그 split view 한 화면

---

## 2026-05-19 추가 — Tempo 실제 연결 (401 인증 문제 해결)

### 증상

- helm 적용 + Alloy receiver Pod 정상 기동
- content-service → Alloy 전송은 성공 (앱 로그 깨끗)
- Alloy → Grafana Cloud Tempo 전송 단계에서 `401 Unauthorized`
- Grafana Explore Tempo Search: 0건

### 원인

- 기존 사용 중이던 Access Policy 토큰이 **read-only** 권한
- 정책 이름: `stack-1606854-ht-read` (scopes: `traces:read`, ...)
- `traces:write` 권한 없는 토큰으로 Alloy가 OTLP push 시도 → 401

### 조치

1. Grafana Cloud → Access Policies → 새 정책 생성
   - scope: `traces:write` (필수)
   - realm: `prod-ap-northeast-0`
2. 토큰 발급 후 `values.yaml`의 `grafana-cloud-traces` destination의 `password` 교체
3. `helm upgrade --version 3.8.7` (revision 8)
4. ⚠️ `alloy-receiver`는 **DaemonSet** (StatefulSet 아님)
   → `kubectl rollout restart daemonset/...` 또는 `kubectl delete pod`로 강제 재시작
5. 재시작 후 `dropped_items` 사라짐 확인

```bash
# Controller 종류 확인
kubectl get daemonset -n monitoring | grep alloy
```

### 학습 포인트

- **Access Policy 토큰의 권한은 이름이 아니라 scope로 결정** — 이전 토큰 이름이 `*-read-*`였지만 발급 시 실제로 어떤 scope를 줬는지가 핵심. 이름만 보고 권한을 짐작하면 안 됨.
- **"Helm upgrade 성공 = Pod 재시작"이 아님** — config(Secret/ConfigMap)이 바뀌어도 기존 Pod는 옛날 메모리 상태 유지 가능. 명시적 rollout restart 필요.
- **chart의 controller 종류(Deployment/StatefulSet/DaemonSet)를 정확히 알아야 재시작 명령이 맞음** — `kubectl rollout restart deployment/x`는 DaemonSet에 안 먹힌다. 먼저 `kubectl get all -n <ns>`로 controller kind 확인할 것.
- **에러는 hop 단위로 끊어서 본다** — 앱 로그 깨끗 ≠ end-to-end OK. Alloy 자체 로그/메트릭(`otelcol_exporter_send_failed_log_records`, `dropped_items`)을 따로 봐야 송신 측 실패가 보인다.

---

## 2026-05-19 — Tempo 연결 완성 (Alloy zipkin receiver 도입)

### 왜 했나

content-service Pod 로그에 다음 에러가 반복:

```
WebClientRequestException: Failed to resolve 'tempo.monitoring' [A(1)]
Spans were dropped due to exceptions.
```

원인: yml의 `TEMPO_ZIPKIN_ENDPOINT` 기본값(`http://tempo.monitoring:9411/...`)은 **자체 호스팅 Tempo** 가정의 placeholder였는데, 실제 운영은 **Grafana Cloud Free + Alloy** 구조라 해당 호스트가 클러스터 DNS에 존재하지 않아 NXDOMAIN. 트레이스가 생성되어도 전송 단계에서 전부 drop.

MDC 패턴(`[traceId=...,spanId=...,userId=11]`)과 Brave instrumentation은 모두 정상 작동 중. 즉 **앱 코드는 트레이스를 만들고 있는데, 받아주는 쪽이 없어서 버려지는 상태**였다.

### 무엇을 했나

| 단계 | 변경 |
|---|---|
| **k8s-monitoring chart values.yaml** | `alloy-receiver: { enabled: true, extraPorts: [9411 zipkin] }` 활성화. nodeSelector를 다른 컴포넌트와 동일하게 worker1(`ip-172-31-45-39`)로 핀. |
| **destinations 배열** | `grafana-cloud-traces` (type=otlp, protocol=grpc) 추가. URL은 chart validation이 알려준 `tempo-prod-20-prod-ap-northeast-0.grafana.net:443`. Tempo는 traces 전용이라 `metrics.enabled: false, logs.enabled: false, traces.enabled: true` 명시. |
| **helm upgrade** | `--version 3.8.7` 로 pin (latest 4.0.3은 destinations 스키마가 array→map으로 breaking change). revision 6으로 적용. |
| **content-service Deployment** | env 추가: `TEMPO_ZIPKIN_ENDPOINT=http://grafana-k8s-monitoring-alloy-receiver.monitoring.svc.cluster.local:9411/api/v2/spans` — base yml 기본값을 override해 클러스터 내부 svc로 직행. |

### 핵심 의사결정

- **Brave/Zipkin 포맷 유지** — 앱 의존성(`micrometer-tracing-bridge-brave` + `zipkin-reporter-brave`) 그대로. OTLP로 전환하려면 의존성 + yml 모두 바꿔야 하는데, k8s-monitoring chart가 zipkin receiver를 기본 노출해주므로 굳이 옮길 이유 없음. 추후 DB/Redis 자동 계측이 필요해질 때 OpenTelemetry agent와 함께 재검토.
- **앱은 클러스터 내부 Alloy로만 전송** — 앱이 Grafana Cloud Tempo로 직접 가지 않음. Alloy가 인증/배치/재시도/포맷 변환(zipkin → OTLP)을 담당. 앱 코드는 모니터링 백엔드(Grafana Cloud)에 무관 — 자체 호스팅으로 전환 시에도 Alloy 설정만 바꾸면 됨.
- **chart는 3.8.7 고정** — 4.x로 점프하면 destinations 스키마 마이그레이션이 필요해서 이번 작업 scope 밖. 별도 정리 작업으로 분리.
- **GitHub egress 차단 잠시 해제** — EC2 SG가 outbound 제한 중이라 helm chart 다운로드가 timeout. 임시로 풀고 `helm upgrade` 후 다시 닫는 운영 패턴. 장기적으로는 NAT GW 또는 사설 helm repo 미러 고려.

### 검증 결과

- `alloy-receiver` Pod `Running 2/2`, Service에 `9411/TCP` 노출 확인
- Alloy 로그: `otelcol.receiver.zipkin.receiver` 컴포넌트 정상 기동, 에러 없음
- content-service 로그에서 `WebClientRequestException` / `Spans were dropped` 사라짐
- Grafana Explore → Tempo → Search by TraceID로 trace 트리 확인 가능

### 다음 단계

- [ ] Grafana 데이터소스 설정: Loki `derivedFields`에 `traceId=(\w+)` 정규식 + Tempo internal link → 로그에서 클릭 한 번으로 trace 점프
- [ ] Tempo 데이터소스의 `tracesToLogsV2` 설정 → trace에서 "Logs for this span" 으로 역방향 점프
- [ ] 24h 정도 데이터 쌓고 Service Graph 확인 (auth-service ↔ content-service 호출 관계)
- [ ] DB/Redis span 자동 계측은 보류 — 필요해지면 datasource-proxy 또는 OTel agent 도입 별도 판단
- [ ] EC2 outbound 영구 정책: GitHub IP 범위만 화이트리스트 또는 NAT GW 도입 (보안 vs 운영 편의 트레이드오프)

---

## 2026-05-18 — Loki / Tempo 검증 계획

메트릭(`/actuator/prometheus`)은 Grafana Cloud에서 확인됨. 다음은 같은 traceId로 **로그(Loki)** 와 **트레이스(Tempo)** 도 묶이는지 확인하는 단계.

### 사전 조건 (이미 적용됨)

| 항목 | 위치 | 상태 |
|---|---|---|
| stdout 로그에 traceId/spanId/userId MDC 출력 | `application.yml` `logging.pattern.level` | OK |
| Zipkin 포맷 span을 Tempo로 전송 | `management.zipkin.tracing.endpoint=http://tempo.monitoring:9411/api/v2/spans` | OK |
| W3C `traceparent` propagation | `management.tracing.propagation.type=w3c` | OK |
| 샘플링 100% | `TRACING_SAMPLING_RATE=1.0` 기본 | OK |
| Brave 브릿지 의존성 | `build.gradle` — `micrometer-tracing-bridge-brave`, `zipkin-reporter-brave` | OK |

### Loki 검증 체크리스트

- [ ] **앱 측 로그 라인 포맷** — Pod stdout에 `INFO  [traceId=abc,spanId=def,userId=NONE] ...` 형식이 찍히는지 직접 확인
  ```bash
  kubectl logs deployment/content-service --tail=20 | grep traceId=
  ```
- [ ] **Alloy podLogs 수집** — k8s-monitoring chart의 `logs.pod_logs.enabled=true` 확인, Alloy Pod 로그에 `loki.write` 컴포넌트 에러 없음
- [ ] **Grafana Explore → Loki 쿼리**
  ```logql
  {namespace="<content-ns>", app="content-service"} |= "traceId="
  ```
  최근 5분 내 라인이 보이면 수집 자체는 OK.
- [ ] **traceId로 한 요청 묶기** — 위 쿼리에서 traceId 하나 골라
  ```logql
  {namespace="<content-ns>", app="content-service"} |= "traceId=<값>"
  ```
  한 요청에서 발생한 로그 시퀀스(JwtFilter → Controller → Service → 결과)가 시간순으로 나오는지.
- [ ] **라벨 카디널리티 점검** — `pod`, `namespace`, `app`, `container`만 라벨. `traceId`, `userId`, `path` 등은 **라인 내용(line filter)** 으로만 검색하고 라벨로 승격하지 않을 것. (Loki 50GB/월 한도 보호.)

### Tempo 검증 체크리스트

- [ ] **앱 → Tempo 연결성** — 클러스터 내부에서 `tempo.monitoring:9411`가 열려 있는지
  ```bash
  POD_IP=$(kubectl get pod -l app=content-service -o jsonpath='{.items[0].status.podIP}')
  kubectl run -n monitoring tempo-test --rm -it --restart=Never --image=busybox -- \
    wget -qO- --post-data='[]' --header='Content-Type: application/json' \
    http://tempo.monitoring:9411/api/v2/spans -O-
  ```
  HTTP 202 응답이면 통과. 404/connection refused면 Tempo 측 zipkin receiver 미활성.
- [ ] **실제 span 생성** — 컨트롤러에 흘려서 trace 만들기
  ```bash
  curl http://<service>/api/feeds/scroll?limit=5
  kubectl logs deployment/content-service --tail=5 | grep traceId=
  ```
  여기서 본 traceId를 그대로 Grafana **Explore → Tempo → Search by TraceID**에 붙여넣기.
- [ ] **스팬 attribute 확인** — root span에 `http.method`, `http.url` (또는 `url.path`), `http.status_code`, `service.name=content-service`가 박혀 있는지. 없으면 Brave instrumentation 누락.
- [ ] **서비스 맵** — Tempo의 Service Graph에 `content-service` 노드가 뜨는지 (수집된 트래픽이 있어야 표시됨, 수 분 지연).
- [ ] **DB/Redis span은 일단 보류** — Brave는 JDBC/Lettuce auto-instrument를 기본 포함하지 않음. 우선 HTTP span만 확인되면 충분. 필요 시 `datasource-proxy` 또는 OpenTelemetry agent 도입은 별도 결정.

### Loki ↔ Tempo 점프(가장 가치 있는 검증)

- [ ] **Tempo 데이터소스 `tracesToLogsV2` 설정** — Grafana Cloud Connections에서 Tempo 데이터소스 편집:
  - Datasource: Loki
  - Tags: `{ key: "service.name", value: "app" }` (Loki 쪽 라벨 키와 매칭)
  - Span start/end time 사용, `${__trace.traceId}` 자동 주입
- [ ] **Loki 데이터소스 derivedFields 설정** — Loki 데이터소스 편집:
  - Name: TraceID, Regex: `traceId=(\w+)`, URL: `${__value.raw}`, Internal link → Tempo
- [ ] **양방향 점프 동작 확인** — Tempo에서 trace 열고 "Logs for this span" 버튼 → Loki 결과 표시. Loki에서 traceId 클릭 → Tempo 트레이스 열림. 이 두 개가 되면 면접에서 "한 요청을 metric→log→trace로 추적할 수 있다" 주장 가능.

### 핵심 의사결정

- **Zipkin 포맷 유지** — Spring Boot 3 + Brave 브릿지 조합에서 가장 간단. OTLP로 전환하려면 `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`로 의존성 교체가 필요한데, Tempo가 Zipkin receiver를 그대로 받아주므로 현 시점에는 가치 없음. 추후 DB/Redis 자동 계측이 필요할 때 OTel agent와 함께 재검토.
- **`traceId`를 Loki 라벨로 올리지 않음** — 라벨로 올리면 모든 요청마다 새 스트림이 생겨 Loki가 즉시 죽는다(high cardinality). 라인 필터(`|= "traceId=xxx"`)로 충분히 빠름.
- **로그 검증을 metric 검증과 분리해서 시간차로 진행** — 두 경로(Alloy → Mimir vs Alloy → Loki)는 같은 Alloy를 쓰지만 컴포넌트가 다르므로 한쪽이 되고 다른 쪽이 안 되는 경우가 흔하다. 메트릭이 떴다고 로그도 자동으로 뜬다고 가정하지 않음.

### 다음 단계

- [ ] 위 체크리스트를 순서대로 한 번 흘려서 모든 항목 통과 확인
- [ ] 통과한 traceId 하나에 대한 스크린샷(Tempo trace + Loki 로그 동시 표시)을 면접 자료로 보관
- [ ] 실패하는 항목은 본 문서에 `## 2026-05-XX — <문제> 해결` 섹션으로 별도 기록

---

## 2026-05-18 — Alloy scrape 경로 검증 + `error` 태그 정리

### 무엇을 했나

| 영역 | 변경 / 확인 |
|---|---|
| `MetricsConfig.java` | `dropRedundantErrorTag()` 추가 — Spring Boot 3 Observation API가 새로 붙이는 `error` 태그가 기존 `exception`과 값이 동일해서 시리즈 차원만 한 단계 곱해줌. `MeterFilter.ignoreTags("error")`로 제거 |
| App 단독 검증 | `kubectl exec deployment/content-service -- wget -qO- http://localhost:8090/actuator/prometheus`로 `http_server_requests_seconds_count` 정상 노출 확인. URI 템플릿(`/feeds/{feedId}` 등) 보존, `/actuator/*` 자기 자신 제외 정상 동작 |
| 네트워크 도달성 검증 | `monitoring` ns에 busybox 일회용 Pod 띄워서 `http://<content-pod-ip>:8090/actuator/prometheus` 호출 → 응답 정상. NetworkPolicy/SecurityGroup 경로 OK |

### 핵심 의사결정

- **`error` 태그를 끄고 `exception`만 남김** — Spring Boot 3에서 둘이 동시에 붙는데 값이 같다. 그대로 두면 같은 정보로 시리즈 수가 (이론상) 두 배 차원으로 곱해질 수 있다. `exception` 쪽이 더 오래되고 대시보드/문서가 풍부해서 그쪽을 유지.
- **검증은 "앱 단독 → 같은 네임스페이스 도달 → Grafana 쿼리" 순서** — 한 단계에서 막힐 때 원인을 좁히기 위해 계층을 분리. Alloy 컨테이너 자체에 wget/curl이 없어서(distroless) 같은 ns에 busybox 띄우는 방식이 가장 단순.
- **`error="IOException"` on 200/SUCCESS는 broken pipe로 해석** — `/feeds/scroll`에서 관측됨. 상태코드는 첫 라인 flush 시점에 결정되므로 200, 이후 본문 write에서 IOException이 observation 태그에만 반영. 실제 오류 아님 → 알림은 `error!="none"`이 아니라 `status=~"5.."` 기준으로 묶을 것.

### 검증 방법

```bash
# 1) App 단독
kubectl exec deployment/content-service -- wget -qO- http://localhost:8090/actuator/prometheus \
  | grep http_server_requests_seconds_count

# 2) 같은 ns에서 Pod IP로 (NetworkPolicy 경로)
POD_IP=$(kubectl get pod -l app=content-service -o jsonpath='{.items[0].status.podIP}')
kubectl run -n monitoring scrape-test --rm -it --restart=Never --image=busybox -- \
  wget -qO- http://$POD_IP:8090/actuator/prometheus | head -5
```

`# HELP application_ready_time_seconds …`가 응답에 보이면 통과.

### 다음 단계

- [ ] content-service Deployment에 `k8s.grafana.com/scrape: "true"` + `metrics.portNumber: "8090"` + `metrics.path: "/actuator/prometheus"` annotation 추가 (k8s-monitoring chart는 opt-in)
- [ ] Grafana Cloud Explore에서 `up{namespace="<content-ns>"}` 및 `http_server_requests_seconds_count{application="content-service"}` 조회로 최종 확인
- [ ] `Dockerfile`에 `EXPOSE 8090` 추가 (문서화 + k8s containerPort 누락 방지)
- [ ] `application-prod.yml`에 `management.server.port: 8090` 명시 (base 상속 의존 제거)

---

## 2026-05-17 — Spring Boot Actuator/Micrometer 표준화

### 무엇을 했나

| 영역 | 변경 |
|---|---|
| `application.yml` (base) | 모든 프로파일 공통 — `spring.application.name=content-service`, `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=30s`, `management.*` 블록(8090 포트 분리, prometheus 노출, SLO 버킷, W3C propagation), `logging.pattern.level`로 traceId/spanId/userId MDC 출력 |
| `MetricsConfig.java` (신규) | **카디널리티 방어** — `http.server.requests`의 `uri` 태그 100개 상한 + `/actuator/*` scrape 노이즈 제외 |
| `JwtFilter.java` | JWT 검증 직후 `MDC.put("userId", ...)`, `finally`에서 `remove`. trace ↔ 로그 ↔ 메트릭 연결의 마지막 1칸 |
| `build.gradle` | `springBoot { buildInfo() }` — `/actuator/info`에 빌드 시간/버전 노출 (배포 식별용) |

이전 단계(`f42a37f`, `5812d55`, `7c5319a` — 2026-05-04~07)에서 prod·dev 프로파일에는 이미 같은 management 블록이 들어가 있었지만, base에 없어서 신규 프로파일이나 local 환경에선 적용되지 않았다. 이번에 base로 끌어올려 어디서 띄우든 동일한 메트릭/트레이스/로깅 패턴이 자동 적용된다.

### 핵심 의사결정

- **관리 포트 8090 분리** — `/actuator/*`를 서비스 포트(8082)와 같이 노출하면 NetworkPolicy로만 차단해야 하고 누락 위험이 있다. 포트 자체를 분리해서 외부 LB는 8082만 알게 한다.
- **`jvm.classes`, `jvm.buffer`, `jdbc`, `tomcat.cache`, `tomcat.servlet` 비활성** — 다 켜면 10k 시리즈 한도가 하루도 못 간다. 운영 가치 대비 카디널리티가 너무 높은 메트릭들.
- **MeterFilter 우선순위 최상** — yml `metrics.enable.*`는 미터 자체 on/off만 가능하고, 태그 카디널리티는 코드 레벨 MeterFilter로만 잡힌다. Grafana Free의 한도 보호는 사실상 이 한 빈에 달려있다.
- **W3C propagation** — auth/chat 서비스도 W3C로 통일하기로 했고, OpenTelemetry/Spring Boot 3 기본 표준이라 미래 호환성 측면에서도 유리. (이전에 B3로 잡혀 있던 것을 W3C로 변경.)
- **`TRACING_SAMPLING_RATE` 기본 1.0** — 사이드 프로젝트 규모라 풀 샘플링이 비용 부담 없음. 트래픽 증가 시 환경변수로 0.1 등으로 낮춘다.

### 검증 방법

```bash
# Pod 띄운 후
curl http://localhost:8090/actuator/health
curl http://localhost:8090/actuator/prometheus | grep -E "http_server|jvm_memory|hikari"
```

로그에 `[traceId=xxx,spanId=yyy,userId=NONE]` 패턴이 찍히고, `/actuator/prometheus` 응답에 `http_server_requests_seconds_*`, `jvm_memory_used_bytes`, `hikaricp_connections_active`가 보이면 Spring 측 작업은 완료.

### 다음 단계

- [ ] K3s worker1에 Grafana Cloud Helm chart(`k8s-monitoring`)로 Alloy DaemonSet + Deployment 설치
- [ ] infra 노드 docker-compose에 `kafka-exporter`/`redis-exporter`/`mongodb-exporter` 추가, VPC 사설 IP로 worker1 Alloy가 scrape
- [ ] 공식 대시보드 import: 17175 (Spring Boot 3 Stats), 11378 (JVM Micrometer), 6417 (K8s Cluster), 763 (Redis), 7589 (Kafka Exporter)
- [ ] Critical 알림 4개: 5xx 비율 / OOMKilled / Kafka consumer lag / p99 SLO 위반
- [ ] 비즈니스 메트릭 추가: `yogurtte.feed.created`, `yogurtte.battle.vote` 등
- [ ] k6 부하 테스트로 N+1 발견 → `@EntityGraph` 적용 → p99 개선 사례 만들기
- [ ] 24h Soak Test로 WebSocket(chat) 누수 검증

---

## 향후 기록 형식

새 변경이 생기면 위에 `## YYYY-MM-DD — 제목` 섹션을 추가한다. 각 섹션은 다음 4개를 포함:

- **무엇을 했나** — 파일 단위로 표
- **핵심 의사결정** — 왜 그렇게 했나 (대안 대비 트레이드오프)
- **검증 방법** — 어떻게 확인했나
- **다음 단계** — 후속 작업 체크박스

git log에 이미 있는 단순 코드 변경 내역보다, "왜 그런 결정을 했는지"와 "면접에서 답변할 컨텍스트"에 무게를 둔다.
