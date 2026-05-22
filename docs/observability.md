# Observability — 메트릭 / 트레이싱 / 로깅

Grafana Cloud Free(메트릭 10k 시리즈, 로그·트레이스 각 50GB/월, retention 14일) 위에 K3s + Alloy 1개로 통합 수집하는 구조. Spring Boot 측 인프라(Actuator + Micrometer + Brave/Zipkin)를 표준화해두고, 트래픽 성장 시 자체 호스팅 LGTM(VictoriaMetrics + Hybrid Pull/Push)으로 마이그레이션할 수 있도록 앱 코드가 모니터링 백엔드에 결합되지 않게 설계한다.

이 문서는 **앱 레벨에서 일어난 결정과 변경**을 누적해서 기록한다. Helm/Alloy/대시보드 설치는 별도 진행.

---

## 2026-05-22 — Saturation 패널 + Alert/Notification 시스템 구축

### 왜 했나

[[2026-05-20]]의 Four Golden Signals 대시보드는 Latency/Traffic/Errors 3개만 완성된 상태였고 **Saturation 영역이 비어있었다**. 메트릭은 [[2026-05-17]] 표준화로 수집 중이지만 **알람 룰이 전무**해서 사실상 "박물관" — 사고가 나도 대시보드를 직접 들여다보지 않으면 모름. 곧 진행할 k6 부하테스트 사이클(N+1 발견 → 개선 → 재부하)에서 어느 자원이 먼저 포화되는지 보려면 Saturation 패널이 선행 조건이고, 면접에서 "메트릭 → 알람 → 발화 → 통보" 한 사이클을 답하려면 Notification까지 닫혀 있어야 함.

부수적으로 디버깅 중 `application` 라벨이 `auth-service`가 아닌 `toy-auth`로 박혀있는 회귀 발견 + `tomcat_threads_*` 메트릭 미노출 발견.

### 무엇을 했나

| 영역 | 변경 |
|---|---|
| **`auth-service/application.yml`** | `spring.application.name` 회귀 수정 — 빌드된 JAR에 `toy-auth`로 박혀있던 것을 `auth-service`로 통일. 메트릭 라벨, Tempo `service.name`, Loki 라벨 모두 영향 |
| **Grafana 대시보드 — JVM Heap 패널** | `sum by (application) (jvm_memory_used_bytes{application=~"$application", area="heap"})`, 단위 bytes(IEC), threshold 384 MiB(75%)/461 MiB(90%) 절대값으로 박음 |
| **Grafana 대시보드 — DB Connection Pool 패널** | HikariCP active/idle/pending 4종 시리즈 한 패널. `hikaricp_connections_pending > 0`이 즉시 비상 시그널 |
| **Grafana 대시보드 — JVM Threads 패널** | `sum by (application) (jvm_threads_live_threads{application=~"$application"})`, threshold 100/300 절대값 |
| **Contact point — Email** | Grafana Cloud 내장 Email integration. 별도 SMTP 불필요. Test 발송 성공 확인 |
| **Notification policy** | Group by: `grafana_folder`, `alertname`, `application`. Group wait 30s / interval 5m / **repeat 4h** |
| **Alert rules (일부 구현, 일부 계획)** | P0: HikariCP Pending > 0 (1m) / 5xx > 5% (5m). P1: P99 > 1s (5m, `uri!~".*ws.*"` SockJS 제외) / Old Gen > 90% (10m). P2: Pod restart (kube-state-metrics 활성화 후) |

### `application` 라벨 회귀 — 진단 과정

**증상**: Grafana 대시보드 `$application` 변수 드롭다운에 `toy-auth`만 표시, `auth-service` 없음.

1. `/actuator/prometheus` 호출 → 메트릭에 `application="toy-auth"` 확인
2. `/actuator/info`의 `build.name="toy-auth"` 확인 → Gradle 프로젝트 이름 잔재
3. ConfigMap `auth-config` 검사 → `SPRING_APPLICATION_NAME` env 없음 (override 아님)
4. JAR 안의 `application.yml`은 `auth-service`로 적혀있는데 빌드된 결과는 다름 → yml 적용 시점/이미지 빌드 시점에서 변형 발생. CI/CD 빌드 사이클 점검 필요

조치: yml 통일 후 rolling restart. 단, **2주간 옛 `toy-auth` 시리즈는 Mimir retention 안에 잔존**하다 자연 소멸.

### 핵심 의사결정

#### (1) Saturation은 단일 지표가 아니라 자원별 3-tier 구조

SRE 4 Signals에서 "Saturation"이 하나의 패널처럼 쓰이지만, **실제 병목은 자원마다 신호가 다르다**:

| 자원 | 병목 신호 | 측정 메트릭 |
|---|---|---|
| 메모리 | Old Gen 고갈 → OOMKilled | `jvm_memory_used_bytes{id="G1 Old Gen"}` |
| DB I/O | Pool 고갈 → request queue 쌓임 | `hikaricp_connections_pending` |
| HTTP 처리 능력 | Tomcat/STOMP executor 포화 | (case-by-case, 아래 (3) 참조) |

→ "saturation %" 한 줄 요약보다 **자원별 패널 분리**가 SRE 관점에서 정확. "Tomcat thread 사용률"같은 일반론적 지표보다 도메인 특화 지표(DB pool, STOMP channel)가 우선.

#### (2) JVM Heap을 비율(%) 대신 절대값(MiB)으로 표시

처음 `used/max * 100`으로 만들었으나 두 문제:
- G1GC가 Eden/Survivor의 `max`를 `-1`로 보고 → `sum`이 의미 망가짐
- "78%"보다 "400 MiB / 512 MiB"가 OOM 임박감을 시각적으로 더 잘 전달

→ **절대값 + max 점선 + threshold를 bytes 절대값(402653184, 483183820)으로** 박는 방식. 부하테스트 시 "실선이 점선 향해 차오르는 시각화"가 직관적.

#### (3) `tomcat_threads_*` 미노출 — 우회 측정으로 분산

`tomcat.threads: true` 설정에도 prometheus endpoint에 `tomcat_threads_*` 미노출. `executor_*` 메트릭은 Spring `@Async`/Scheduler 풀이지 Tomcat HTTP connector 풀 아님.

→ "Tomcat thread 포화"를 직접 측정 불가. 대안: **JVM Live Threads (절대값)** + **HikariCP saturation** + (chat 한정) **STOMP channel executor**로 분산 측정. Tomcat thread는 결국 DB pool 고갈의 후행 지표(thread가 DB에서 대기)라 우회가 무의미하진 않음.

⚠️ **솔직한 한계**: 정확한 Tomcat HTTP saturation은 아직 측정 못 함. 진짜 필요해지면 Spring Boot 버전별 Tomcat metrics binder 검토.

#### (4) SockJS WebSocket fallback이 P99 메트릭 오염

P99가 평소 10초로 튀는 이슈 발견. Tempo에서 trace 추적해보니 **`/api/ws/{server_id}/{session_id}/xhr` 경로의 SockJS long-polling fallback이 의도적으로 25초간 연결 유지**하는 정상 동작.

→ 즉시 조치: P99 PromQL에 `uri!~".*ws.*"` 필터 추가. 코드 레벨 영구 조치(MetricsConfig에 SockJS URI 필터링)는 별도 작업으로 분리. **면접 답변**: "P99 10초가 진짜 느린 게 아니라 SockJS의 정상 동작이라는 걸 trace로 검증 — 메트릭 단독으로 false alarm 일으키는 시그널을 trace로 교차검증한 사례."

#### (5) Alert Pending Period — 알람 종류별 차등

| 알람 | Pending | 근거 |
|---|---|---|
| HikariCP Pending > 0 | 1m | 즉시 비상 — pool 고갈은 곧장 사용자 영향 |
| 5xx > 5%, P99 > 1s | 5m | 단발 spike 무시, 지속성 확인 후 발화 |
| Old Gen > 90% | 10m | Major GC 후 떨어질 수 있음, 지속 우상향만 알람 |

"알람 피로(alert fatigue) 방지"가 SRE 기본 — 1분 spike에 다 발화하면 진짜 사고 시 묻힌다.

#### (6) Repeat interval 4h

미해결 알람 재발송 주기. 1h는 너무 잦아 피로, 24h는 잊혀짐. **4h = 근무 시간 중 6번** = 의식 가능한 빈도. 사이드 프로젝트 규모 적합.

### 검증 방법

```bash
# 1) application 라벨 통일 확인
kubectl exec deployment/auth-service -- wget -qO- http://localhost:8090/actuator/prometheus \
  | grep -oE 'application="[^"]+"' | sort -u
# application="auth-service" 단독으로 나와야 함

# 2) Grafana Cloud 도달 확인 (Explore)
#    group by (application) (http_server_requests_seconds_count)
#    → auth-service, chat-service, content-service 세 값 모두 표시

# 3) Saturation 패널 정상 동작 (대시보드)
# - JVM Heap: 세 서비스 모두 시계열, 0 ~ 512 MiB Y축
# - Hikari: chat-service는 메트릭 없음(NoSQL만 사용, 정상), auth/content는 idle=10

# 4) Alert 시스템 — Email contact point
# Alerting → Contact points → email-me → Test 버튼
# → 본인 이메일 (스팸함 포함) 확인
```

### 부하테스트 직전 체크리스트

- [x] Saturation — JVM Heap 패널 (절대값, 512 MiB 상한)
- [x] Saturation — DB Connection Pool 패널 (active/idle/pending 분리)
- [x] Saturation — JVM Threads 패널 (live threads, 절대값)
- [x] Email Contact point 등록 + Test 발송 성공
- [x] Notification policy (default → email-me)
- [ ] **GC Activity 패널** — `rate(jvm_gc_pause_seconds_sum[30s])` by action
- [ ] **JVM Thread States 패널** — stacked, blocked 시리즈 강조
- [ ] **Old Gen 별도 패널** — `jvm_memory_used_bytes{id="G1 Old Gen"}`
- [ ] **Alert Rule — HikariCP Pending > 0** 발화 테스트 (실 발화 확인까지)
- [ ] **부하테스트용 별도 대시보드** (Refresh 5s, Time range 15m)

### 다음 단계

- [ ] GC Activity / Thread States / Old Gen 패널 추가 (부하테스트 1순위 시각화)
- [ ] Alert Rule 4종 실 구현: HikariCP Pending → 5xx → P99 → Old Gen 순서로
- [ ] k6 가벼운 부하(10 VUs, 1m)로 어느 패널이 가장 먼저 튀는지 관찰
- [ ] N+1 발견 → `@EntityGraph` 개선 → 재부하 → 골든 시나리오 완성 ([[2026-05-17]] 다음 단계의 연장)
- [ ] content-service heap이 평상시 400 MiB대 — sawtooth(정상) vs 우상향(memory leak) 6h 그래프 확인
- [ ] content-service `Max` 컬럼 610 MiB 이상값 원인 추적 (Eden/Survivor `-1` 합산 영향 또는 컨테이너 limit 동적 조정)
- [ ] MetricsConfig에 SockJS URI 필터 추가 — 대시보드 쿼리 레벨 임시 처리를 코드 레벨로 영구화
- [ ] `tomcat_threads_*` 메트릭 부재 원인 추적 — Spring Boot 3.x Tomcat metrics binder 검토 (낮은 우선순위)
- [ ] 부하테스트 실제 발화 검증되면 별도 섹션(`2026-05-23 — 부하테스트 사이클 1회차`)으로 기록

---

## 2026-05-21 — toy-chat 표준 이식 + WebSocket 관측성

### 왜 했나

3-tier MSA(auth → content → chat)의 Service Graph를 Tempo에서 보려면 모든 서비스가 **같은 traceId/span 컨벤션**으로 telemetry를 내야 한다. toy-content/toy-auth는 표준화 완료, 마지막 남은 toy-chat이 가장 결핍이 컸음:

| 항목 | toy-chat 이전 상태 |
|---|---|
| tracing 의존성(brave bridge, zipkin reporter) | 부재 — trace 자체가 생성 안 됨 |
| `application.yml` management 블록 | 거의 부재 (prometheus만 expose) |
| graceful shutdown / app.name | 부재 |
| MetricsConfig (MeterFilter) | 부재 — URI 카디널리티 무방어 |
| JwtFilter MDC | 부재 |
| **WebSocket 메시지의 MDC** | **부재** — 채팅 메시지 로그가 userId=NONE으로 찍힘 |
| WebSocket 메트릭 | 부재 |

WebSocket이 끼어 있어서 HTTP 서비스와 똑같이 가면 부족함 — 메시지 처리 스레드가 다른 풀이라 MDC가 자동으로 따라오지 않는다.

### 무엇을 했나 (toy-chat)

| 파일 | 변경 |
|---|---|
| `build.gradle` | `micrometer-tracing-bridge-brave` + `zipkin-reporter-brave` 의존성 추가 (trace 생성 가능), `springBoot { buildInfo() }` |
| `src/main/resources/application.yml` (base) | `spring.application.name=chat-service`, `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=30s`, management 블록 전체(8090 분리, prometheus 노출, SLO 버킷, W3C tracing, zipkin endpoint), `logging.pattern.level`로 traceId/spanId/userId MDC 패턴 |
| `app/config/MetricsConfig.java` (신규) | MeterFilter 4종 — URI 카디널리티 상한(100), `/actuator/*` 자기 scrape 제외, `error` 태그 제거, 404/405 → `UNMATCHED` 통합 |
| `app/auth/filter/JwtFilter.java` | JWT 검증 직후 `MDC.put("userId", ...)`, `try-finally`로 remove. HTTP 요청 경로용. |
| `app/websocket/interceptor/StompChannelInterceptor.java` | **`preSend`에서 `sessionAttributes.userId` → `MDC.put`, `afterSendCompletion`에서 finally로 `MDC.remove`**. WebSocket 메시지 경로용. |
| `app/config/WebSocketMetricsConfig.java` (신규) | `SimpUserRegistry.getUserCount()` 기반 `websocket.active_users` Gauge 1개 |

### 핵심 의사결정

#### (1) WebSocket MDC 전파를 ChannelInterceptor의 preSend에 둠

선택지가 두 가지였다:
- **A) `StompConnectHandler`에서 한 번만 put** — 연결 시점에 박고 끝. 간단.
- **B) `ChannelInterceptor.preSend`에서 매 메시지마다 put + afterSendCompletion에서 remove** — 채택.

A를 안 한 이유: STOMP 메시지는 **Spring Messaging의 별도 스레드 풀**(`clientInboundChannel.taskExecutor`)에서 처리된다. CONNECT 한 번에 MDC를 박아도 그 다음 SEND/SUBSCRIBE 메시지는 풀 안의 **다른 스레드**가 처리하니까 MDC가 따라오지 않는다. 풀의 스레드 N개 × 동시 사용자 M명이 섞여서 어느 스레드가 누구의 MDC를 갖고 있는지 보장이 안 됨. → 매 메시지마다 다시 박는 게 안전.

#### (2) `MDC.remove`를 `finally`에 두는 이유 (재확인)

`preSend`에서 `put`만 하고 안 지우면, 메시지 처리 끝난 스레드가 풀로 반납되는데 MDC는 그대로 남는다. 다음 메시지가 그 스레드를 잡으면 다른 사용자 행동이 이전 사용자 ID로 로깅됨. 같은 메시지 풀 안에서도 동일 문제가 재현 — Tomcat HTTP 스레드 재사용과 본질적으로 같다.

#### (3) 활성 사용자 메트릭은 SimpUserRegistry로 — 직접 카운트 안 함

Phase 3(자체 카운터)를 선택지 위에 두고 비교한 끝에 옵션 1 채택. 근거:
- **Spring이 이미 카운팅 중**이므로 `Gauge`로 read-only 노출만 하면 됨 → 5줄, hot path 영향 0
- 사이드 프로젝트 트래픽에서 Phase 3가 만들 부가 지표(메시지 throughput, SUBSCRIBE 카운터, 세션 지속시간 히스토그램)는 거의 0이거나 변동 없음 — "있는데 의미 없는 메트릭"이 카디널리티만 차지
- **확장 경로 보존**: 세션 단위 카운트가 필요해지면 `StompConnectHandler/DisconnectHandler`에서 `AtomicInteger` 증감 + `Gauge` 한 줄 추가로 보강 가능. 지금 안 해도 됨 = YAGNI.
- 면접 답변: "`SimpUserRegistry`를 발견해 5줄로 활성 사용자 메트릭 노출. 세션 단위까지 필요해지면 STOMP 핸들러에 카운터 추가로 확장 — 트래픽이 그 정도로 차야 의미 있는 메트릭이라 지금은 보류."

#### (4) 카운트 단위: 유저 vs 세션

`SimpUserRegistry.getUserCount()`는 **고유 사용자** 카운트. 1명이 PC + 모바일 2대 접속하면 1로 잡힘. 우리 도메인에서는 "지금 채팅 가능한 사람 수"가 의미 있는 지표라 유저 단위로 OK. 다중 디바이스가 비즈니스 핵심이 되면 세션 단위로 보강.

#### (5) HTTP shouldNotFilter에서 `/api/ws`는 어차피 통과

JwtFilter는 `/api/ws` 경로를 `shouldNotFilter`로 빠져나가게 둠 — WebSocket 핸드셰이크 자체는 HTTP 필터 체인을 안 거치고 (`HandshakeInterceptor`로 따로 처리), 인증은 STOMP CONNECT 시점의 `StompConnectHandler`에서 JWT 검증. JwtFilter의 MDC.put은 일반 HTTP REST API 호출용이고, WebSocket 메시지 MDC는 ChannelInterceptor가 담당 — 두 경로가 깔끔히 분리됨.

### 검증 방법

```bash
# 1) 컴파일
./gradlew compileJava

# 2) Pod 재배포 후 trace 전송 에러 사라지는지
kubectl logs deployment/chat-service --tail=50 | grep -E "WebClient|Spans were dropped"

# 3) HTTP REST 호출 로그에 traceId/userId 찍히는지
kubectl logs deployment/chat-service --tail=30 | grep -oE 'traceId=[a-f0-9]+,spanId=[a-f0-9]+,userId=[^]]+'

# 4) ⭐ WebSocket 메시지 처리 로그에도 userId 찍히는지 (이게 새로 추가된 핵심)
#    채팅 메시지 한 번 보내고 메시지 처리 로그 확인 — userId=NONE이 아니라 실제 ID여야 함
kubectl logs deployment/chat-service --tail=30 | grep -E "ChatStomp|SEND" | head -3

# 5) /actuator/prometheus에 활성 사용자 메트릭 노출 확인
kubectl exec deployment/chat-service -- wget -qO- http://localhost:8090/actuator/prometheus \
  | grep "websocket_active_users"
```

### Grafana 대시보드에서 보기

- **PromQL**: `websocket_active_users{application="chat-service"}` — 시계열 그래프
- **알람 후보**: 평소 대비 50% 이상 감소 5분 지속 → 채팅 연결 장애 의심

### 다음 단계

- [x] chat-service Deployment YAML 업데이트 — 관리 포트 8090 추가, probe/scrape 경로 `/actuator/...`로 변경, `TEMPO_ZIPKIN_ENDPOINT` env, `terminationGracePeriodSeconds: 60` (WebSocket 세션 cleanup 여유)
- [ ] `kubectl apply` 후 검증: trace 전송 에러 없음, WS 메시지 로그에 userId MDC, `websocket_active_users` 메트릭 노출 확인
- [ ] Tempo에서 한 traceId 추적: REST 호출 시작 → auth(JWT 검증) → content(피드 조회) → chat(메시지 발송) 한 trace로 묶이는지
- [ ] `websocket.active_users` Grafana 패널 추가 (Four Golden Signals 대시보드에 한 panel 추가)
- [ ] 트래픽 데이터 1주일 모은 뒤 Phase 3 진입 가치 재평가 — 세션 단위 카운트, 메시지 throughput, CONNECT 실패율 중 실제로 알람 후보가 되는 게 있는지 보고 결정
- [ ] (별도 작업) 카프카 컨슈머 lag 메트릭 — chat-service가 kafka로 메시지 받아 처리하는데 lag 모니터링이 없음. Micrometer Kafka binder 활성화 필요

---

## 2026-05-21 (오후) — JwtFilter mgmt 포트 화이트리스트 + RequestLoggingFilter 정책

위 오전 작업 배포 직전 추가로 발견/정리한 항목들.

### (1) JwtFilter가 관리 포트(8090) actuator 요청을 가로채는 문제

**증상**: toy-auth 배포 직후 K8s probe 실패. 로그에 다음이 반복:
```
[http-nio-8090-exec-1] ... c.e.t.app.common.filter.JwtFilter
- >>>> JwtFilter 진입, URI: /actuator/health/readiness
```

readiness 경로가 JwtFilter를 거치는데 토큰이 없으니 401 반환 → probe 실패 → Pod NotReady.

**원인 분석**
- `JwtFilter`가 `@Component`로 등록되면 Spring Boot가 `FilterRegistrationBean`을 자동 생성. 이 등록이 메인 ServletContext뿐 아니라 관리 서버 컨텍스트(별도 Tomcat connector)에도 적용되는 케이스가 존재.
- 적용 여부는 **Spring Boot 버전에 따라 다름** — 3.2.x는 관리 컨텍스트에도 자동 등록되는 경향, 3.4+는 분리되는 경향. 거기에 `addFilterBefore(jwtFilter, ...)`로 SecurityFilterChain에 같은 빈을 추가하면 양쪽 컨텍스트에 모두 적용될 가능성이 커짐.
- 관리 포트 actuator는 `context-path`(/api)가 적용되지 않아 경로가 `/actuator/health/readiness`로 옴 — 기존 `shouldNotFilter`의 `/api/actuator` 화이트리스트로는 막아지지 않음.

**왜 content-service는 작동했나**
- content-service: Spring Boot 3.4.1 + SecurityConfig가 `JwtAuthenticationFilter`라는 **별도 빈**을 만들어 Security chain에 추가. `@Component` JwtFilter는 servlet container 자동 등록만 됨. 3.4의 새 동작상 관리 컨텍스트에 자동 적용 안 됨.
- auth-service: Spring Boot 3.2.1 + `@Component` JwtFilter를 직접 `addFilterBefore`로 Security chain에도 추가. 두 경로로 등록되면서 관리 포트에도 필터가 붙음.
- chat-service: 3.5.3 + Spring Boot 동작이 어느 쪽으로 바뀔지 불명 → defensive하게 두는 게 안전.

**조치**: 세 서비스 모두 `shouldNotFilter`에 `/api/actuator` + `/actuator` 두 경로 모두 화이트리스트.
```java
return path.startsWith("/api/actuator")     // main port (8082/8084) + context /api
    || path.startsWith("/actuator")          // mgmt port (8090), context 없음
    || ...
```
주석에 "Spring Boot 버전이 바뀌어도 K8s probe가 401을 받지 않도록" 명시.

### (2) RequestLoggingFilter — content-service에만 있고 다른 서비스엔 없는 이유 정리

**파일**: `toy-content/src/main/java/com/example/toycontent/app/common/filter/RequestLoggingFilter.java`

```java
if (status >= 500)       → log.error
else if (status >= 400)  → log.warn
else if (elapsed > 1s)   → log.warn ("HTTP-SLOW")
else                     → log.info
```

`OncePerRequestFilter`로 모든 HTTP 요청에 대해 method/URI/status/elapsed 1줄 출력. JwtFilter 뒤에 실행돼서 MDC userId가 이미 박힌 상태로 로그가 찍힘.

**결정**: 다른 두 서비스(auth, chat)에는 **추가하지 않음**. 근거:
- metric + trace 두 채널이 같은 정보를 이미 가지고 있음. 필터는 "Loki에서 1줄 grep으로 보는 단축 채널" 역할 — 정보 중복.
- Loki 50GB/월 한도가 빠듯한 사이드 프로젝트 규모에서 모든 요청을 추가 로그로 적재하는 비용이 큼.
- content-service만 두는 이유: 트래픽 70% 처리하는 메인 서비스라 "Loki에서 사용자별 요청 timeline 빠르게 grep" 유스케이스가 있음. auth는 인증 요청만, chat은 WebSocket 위주라 REST 요청 자체가 적음.

**확장 기준**: 운영 중 "특정 요청 패턴(예: 4xx의 client IP/UA)을 Loki에서 자주 grep해야 한다"는 요건이 생기면 그때 해당 서비스에 추가. 지금은 YAGNI.

### (3) chat-service Deployment YAML 업데이트 — WebSocket 특화 고려사항

기존 chat 배포는 port 8084 단일, context `/api/actuator/...`로 probe. 새 표준 적용으로 6곳 변경:

| 항목 | 기존 | 변경 |
|---|---|---|
| annotation `metrics.portNumber` | `"8084"` | `"8090"` |
| annotation `metrics.path` | `/api/actuator/prometheus` | `/actuator/prometheus` |
| `ports` | `containerPort: 8084` | `8084`(http) + `8090`(mgmt) |
| env | 없음 | `TEMPO_ZIPKIN_ENDPOINT` 추가 |
| livenessProbe / readinessProbe | port `8084`, path `/api/actuator/...` | port `8090`, path `/actuator/...` |
| `terminationGracePeriodSeconds` | 미설정(기본 30s) | **`60s`** |

**WebSocket 특화 — `terminationGracePeriodSeconds: 60`**
- 일반 HTTP 서비스는 30s면 충분. 채팅은 **동시접속 300명 기준 WebSocket 상시 연결**이라 30s 안에 모든 세션에 close frame 보내고 client ack 받기 빠듯.
- SIGTERM → Spring graceful shutdown 시작 → 모든 WS 세션에 close frame 전송 → 30s 안에 못 끝내면 K8s가 SIGKILL → TCP RST → client는 연결 끊긴 줄 모르고 한참 후 재연결.
- 60s면 안전 마진 확보. 트래픽 더 늘면 `spring.lifecycle.timeout-per-shutdown-phase`도 같이 늘려야 함 (현재 30s).

### 오늘 커밋 정리

| 저장소 | 브랜치 | 커밋 | 내용 |
|---|---|---|---|
| toy-chat | develop | `8f0822a` | observability 6개 파일 (build.gradle / yml / MetricsConfig / JwtFilter / StompChannelInterceptor / WebSocketMetricsConfig) |
| toy-content | main | `1a3bccf` | docs/observability.md 2026-05-21 섹션 + JwtFilter `/actuator` 화이트리스트 추가 |

(이전 푸시들: toy-auth develop `d86763e` MetricsConfig + JwtFilter MDC + buildInfo, content develop `5af854e` 핫피드/리액션 등)

---

## 2026-05-20 — toy-auth 옆 서비스에 동일 표준 이식

### 왜 했나

Tempo Service Graph에서 `auth-service ↔ content-service` 호출 관계를 보려면 양쪽 서비스가 **같은 traceId를 공유**하고 **같은 라벨 컨벤션**으로 메트릭/로그를 내야 함. content-service만 표준화돼 있고 auth-service는 누락분이 있어서 동일한 3종 패치를 이식.

조사 결과 toy-auth는 application.yml(`spring.application.name=auth-service`, `server.shutdown=graceful`, management 블록, `logging.pattern.level` traceId/spanId/userId)과 의존성 4종(actuator + prometheus + brave + zipkin-reporter)이 이미 적용된 상태였음(`168ad0b`). **앱 코드 3가지가 빠져 있었음**.

### 무엇을 했나 (toy-auth-user-region)

| 파일 | 변경 |
|---|---|
| `src/main/java/com/example/toyauth/app/config/MetricsConfig.java` (신규) | MeterFilter 4종: URI 카디널리티 상한(100), `error` 태그 제거, `/actuator/*` 자기 scrape 제외, 404/405 → `UNMATCHED` 통합. toy-content와 동일 구현. |
| `src/main/java/com/example/toyauth/app/common/filter/JwtFilter.java` | JWT 검증 직후 `MDC.put("userId", ...)`, `try-finally`로 `MDC.remove` 보장. |
| `build.gradle` | `springBoot { buildInfo() }` 추가 — `/actuator/info`에 빌드 시간/버전 노출. |

K8s Deployment에는 `TEMPO_ZIPKIN_ENDPOINT` 환경변수로 cluster-internal Alloy receiver(`grafana-k8s-monitoring-alloy-receiver.monitoring.svc.cluster.local:9411/api/v2/spans`)를 주입. 두 서비스가 동일 endpoint를 보므로 Tempo Service Graph가 자동 구성됨.

### 핵심 의사결정

- **MDC.remove를 `finally`에 두는 이유** — Tomcat은 스레드 풀에서 스레드를 재사용한다. `MDC.put`만 하고 안 지우면 다음 요청이 같은 스레드를 잡았을 때 이전 사용자의 userId를 그대로 물려받아 **다른 사람 행동이 내 ID로 로깅**되는 사고가 난다. `return`/예외/401 어떤 경로로 빠지든 무조건 청소되도록 try-finally. 개인정보/감사 로그 무결성에 직결되는 표준 패턴.
- **MDC.put은 try 밖, remove만 finally 안** — put 자체는 절대 throw 안 함. 예외 위험이 있는 `filterChain.doFilter()`만 try로 감싸면 충분. 불필요하게 범위를 넓히지 않음.
- **두 서비스의 management 포트를 8090으로 통일** — Pod마다 별도 network namespace라 같은 노드에 떠도 충돌 없음. 일관된 포트가 NetworkPolicy/SG/scrape annotation 룰을 단순하게 만듦.
- **MetricsConfig를 두 서비스에 똑같이 복제** — 살짝 DRY 위반이지만 공용 라이브러리 추출은 과한 추상화. 두 서비스가 분리된 배포 단위인 한 코드 복제(< 100 lines)가 더 단순.
- **chart는 그대로 두고 앱 측만 패치** — k8s-monitoring chart는 이미 alloy-receiver(zipkin 9411)를 노출 중([[2026-05-19]]). 인프라는 무변경, 앱만 같은 endpoint를 보도록 환경변수 주입.

### 검증 방법

```bash
# 1) auth-service Pod 재배포 후 trace 전송 에러 사라졌는지
kubectl logs deployment/auth-service --tail=50 | grep -E "WebClient|spans"

# 2) MDC 패턴 정상 출력
kubectl logs deployment/auth-service --tail=20 | grep -oE 'traceId=[a-f0-9]+,spanId=[a-f0-9]+,userId=[^]]+'

# 3) auth → content 호출 trace가 같은 traceId로 묶이는지
#    Grafana Explore → Tempo → 임의 traceId 검색 → span 트리에 두 service.name 모두 표시되는지
```

### 다음 단계

- [ ] auth-service Deployment에 `TEMPO_ZIPKIN_ENDPOINT` 환경변수 적용 + rolling restart
- [ ] Tempo Service Graph에서 `auth-service → content-service` 호출 화살표 확인 (트래픽 누적 수 분 필요)
- [ ] 24h 후 양 서비스의 P50/P95/P99 latency 분포 비교 — chat-service까지 같은 표준으로 묶이면 3-tier MSA 관측성 완성
- [ ] toy-chat 동일 패치 적용 (`MetricsConfig` 복제 + MDC put/remove)

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
