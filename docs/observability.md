# Observability — 메트릭 / 트레이싱 / 로깅

Grafana Cloud Free(메트릭 10k 시리즈, 로그·트레이스 각 50GB/월, retention 14일) 위에 K3s + Alloy 1개로 통합 수집하는 구조. Spring Boot 측 인프라(Actuator + Micrometer + Brave/Zipkin)를 표준화해두고, 트래픽 성장 시 자체 호스팅 LGTM(VictoriaMetrics + Hybrid Pull/Push)으로 마이그레이션할 수 있도록 앱 코드가 모니터링 백엔드에 결합되지 않게 설계한다.

이 문서는 **앱 레벨에서 일어난 결정과 변경**을 누적해서 기록한다. Helm/Alloy/대시보드 설치는 별도 진행.

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
