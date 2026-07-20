# Tracing 커버리지 검증 — 경계 열거 체크리스트

> "댓글 요청 하나로 커버리지를 일반화한 것 아니냐(n=1)"는 질문에 대한 답이자,
> Grafana(Tempo)에서 경계별로 대조 확인할 때 쓰는 목록.
> 마지막 갱신: 2026-07-20. 상세 이력은 [observability.md](observability.md).

## 검증 프레임 — 왜 요청 수를 세지 않았나

계측 커버리지는 **요청의 함수가 아니라 계측 지점(라이브러리 × 경계)의 함수**다. JDBC 계측은 켜져 있거나 꺼져 있거나지, 요청마다 달라지지 않는다. 따라서 검증 단위는 "요청 N개"가 아니라 **경계 유형의 전수 열거**이고, 대표 경로로 댓글 작성을 고른 이유는 그것이 시스템의 경계 유형을 한 요청으로 최다 통과하는 경로이기 때문이다:

```
POST /comments (HTTP 인입)
 → INSERT×N (JDBC)
 → AFTER_COMMIT 이벤트 + @Async 경계 (notification-publish)
 → Kafka producer (publish user.notifications)
 → chat consume → Mongo insert / Redis 조회 / WS·FCM 발송
```

## 1. 경계 전수 목록 — 계측 스위치와 검증 상태

| # | 경계 | 계측 수단 (코드 위치) | 기대 span | 상태 | 근거 |
|---|---|---|---|---|---|
| 1 | HTTP 인입 (server) | Spring MVC 자동 계측 | `POST /api/...` server span | ✅ 실측 | 댓글 E2E 워터폴 |
| 2 | JDBC (MySQL) | `datasource-micrometer-spring-boot` | connection / query span | ✅ 실측 | 같은 워터폴의 INSERT×N |
| 3 | 이벤트 리스너 (AFTER_COMMIT) | `@Observed("notification.publish")` — `NotificationEventListener` | `notification-publish` | ✅ 실측 | 요청 트리에 연결 확인 (2026-07-20) |
| 4 | `@Async` 경계 | `AsyncConfig`의 TaskDecorator 컨텍스트 전파 | 3번 span이 요청 traceId 유지 | ✅ 실측 + 전수 열거 | 아래 §2 — content에 `@Async`는 1곳뿐 |
| 5 | Kafka producer | `KafkaConfig.kafkaTemplate` `setObservationEnabled(true)` | `publish user.notifications` + 헤더 전파 | ✅ 실측 | 같은 워터폴 (③ 관문 통과) |
| 6 | Kafka consumer (chat) | 리스너 팩토리 5개 `setObservationEnabled(true)` (`cab6741`) | `consume` span, traceId 연속 | 🔄 코드 완료, 배포 후 워터폴 대기 | observability.md 2026-07-20 |
| 7 | 서비스 간 HTTP (content→user) | 자동 구성 `WebClient.Builder` 주입 (`1a7fb41`) — `ExternalUserApiClient` | client span + traceparent 전파 | 🔄 수리 완료, 워터폴 실측 대기 | 정적 빌더 시절엔 단절이었음 |
| 8 | Redis(Lettuce) 커맨드 | `TracingConfig` `MicrometerTracing` (`1a7fb41`) | 캐시/ShedLock 커맨드 span | 🔄 켜짐, 워터폴 실측 대기 | — |
| 9 | chat 내부 (Mongo / Redis / 자체 producer / `@Observed` 3개) | toy-chat `ObservabilityConfig` 외 (`cab6741`) | mongo insert, `notification.process` 등 | 🔄 6번과 같은 배포 검증에 포함 | — |
| 10 | 메서드 계측 (`@Observed`) | `TracingConfig` ObservedAspect | 스케줄러·알림 발행 span | ✅ 동작 확인 | 스케줄러 trace가 Loki↔Tempo 점프 검증에 등장 (2026-05-20) |

**한 줄 요약**: trace로 수집 가능한 경계 10종을 열거했고, content 쪽 1~5는 댓글 E2E 워터폴로 실측 완료. 6~9는 코드는 닫혔고 toy-chat 배포 후 같은 워터폴 1회로 일괄 실측된다.

## 2. `@Async` 전수 열거 — grep으로 닫음

`grep -rn "@Async" src/main/java` 결과 **content의 `@Async`는 `NotificationEventListener.handle` 1곳**이 전부다. `NotificationService.sendSafely` 12곳은 전부 이 리스너 경유(동기 호출)이며 별도 executor를 타지 않는다. 즉 "@Async fan-out 다른 데는요?"의 답: **다른 데가 없다**(열거 근거가 grep 전수 검색). 이 1곳은 TaskDecorator 전파 + 실측으로 검증 완료.

- 커스텀 executor: `notificationExecutor` (`AsyncConfig`) 1개.
- 새 `@Async` 지점을 추가하면 이 문서와 TaskDecorator 적용 여부를 함께 확인할 것.

## 3. 비-HTTP 진입점 전수 열거 — 스케줄러 9개

"HTTP 발 트레이스만 본 것 아니냐"에 대한 답. `@Scheduled` 전수:

| 스케줄러 | 메서드 수 | root span (`@Observed`) | 상태 |
|---|---|---|---|
| `BattleHotScoreScheduler` | 2 | ✅ `scheduler.battle.hotScore.*` | ✅ 실측 — 2026-05-20 Loki↔Tempo 검증에서 이 스케줄러의 trace로 양방향 점프 확인 |
| `FeedHotScoreScheduler` | 2 | ✅ `scheduler.feed.hotScore.*` | 동일 패턴 — 계측 동일하므로 커버 |
| `BattleDeadlineNotificationScheduler` | 2 | ✅ `scheduler.battle.notification.*` | 동일 패턴. 단 이 경로는 Kafka 발행까지 이어지므로 chaos 채록 시 스케줄러발 트레이스 표본으로 적합 |
| `FeedTrendingScheduler` | 1 | ❌ **없음** | ⚠️ root span 부재 — 실행돼도 트레이스가 안 생김 (JDBC span이 붙을 트리가 없다) |
| `ProductPopularityScheduler` | 2 | ❌ **없음** | ⚠️ 동일 사각지대 |

→ **보강 필요**: `FeedTrendingScheduler`·`ProductPopularityScheduler`에 `@Observed` 추가 (각 1줄, 기존 네이밍 컨벤션 `scheduler.<도메인>.<작업>` 따름). 이 체크리스트 작성 과정의 전수 열거에서 발견된 실제 구멍.

Kafka consumer 진입점은 chat 소관(리스너 5개, DLQ 포함 observation 켜짐 — 재시도/DLQ 경로 실측은 §4).

## 4. 프레임이 못 막는 구멍 3개 — chaos 문항 채록에 얹어 실측

| 공격 | 상태 | 채우는 방법 (별도 작업 아님 — chaos 문항 생산에 포함) |
|---|---|---|
| "에러 경로도 span에 남아요?" | ⚠️ 미검증 — 확인한 건 전부 정상 흐름 | chaos 문항 2(슬로우 쿼리/강제 예외) 채록 시 error 태그·exception 기록 확인. ~10분 |
| "컨슈머 재시도·DLQ 경로는요?" | ⚠️ 스위치는 켜짐(dlq 팩토리 포함), 실측 없음 | chaos 문항 중 컨슈머 실패 시나리오에서 DLQ 트레이스 확인 |
| "스케줄러발 트레이스는요?" | ⚠️ 부분 — BattleHotScore 1건 실측(5/20), 2개 스케줄러는 root span 자체가 없음 | §3 `@Observed` 보강 후 각 1회 트레이스 확인. 문항 채록 체크리스트에 "예상 span 전부 존재?" 한 줄 추가 |

## 5. 구조적으로 trace 불가 (열거상 "커버 안 함"이 아니라 "커버 불가"로 분류)

- STOMP 브로커 → 클라이언트 실제 전달 — 메트릭(`websocket_active_users` 등) 담당
- FCM SDK 내부 HTTP — `notification.push.dispatch` span이 소요 시간으로 커버
- 프론트엔드 — 추후 traceparent 전파 + OTel JS로 확장 가능 (W3C 표준이라 Brave 호환)

## 6. Grafana(Tempo)에서 경계별 대조하는 법

검증 트래픽은 [tracing-coverage.postman_collection.json](tracing-coverage.postman_collection.json)으로 흘린다 — Postman에 import 후 `email`/`password` 변수만 채우고 Run collection 하면 로그인 → accessToken 자동 주입 → 피드 채취 → 댓글 작성(핵심 채록) → 캐시/에러 경로 순으로 실행된다. 운영 검증이면 `authBaseUrl`/`contentBaseUrl`을 배포 도메인으로 교체.

댓글 1건 작성 후 해당 traceId 워터폴에서 아래가 **한 트리**로 보이면 1~9 전부 통과:

```
POST /api/... (content-service, server)
├─ INSERT×N (JDBC)
├─ GET user-service (client span — §1-7 확인 지점)
├─ redis 커맨드 (§1-8 확인 지점)
└─ notification-publish (@Async 경계 유지 — traceId 동일이면 §1-4 통과)
   └─ publish user.notifications (producer)
      └─ consume (chat-service — §1-6 확인 지점)
         └─ notification.process
            ├─ mongo insert
            ├─ notification.ws.send → redis 조회
            └─ notification.push.dispatch
```

- 서비스 경계 확인: TraceQL `{ resource.service.name = "content-service" } && { resource.service.name = "chat-service" }` — 두 서비스가 같은 trace에 있는 것 자체가 4·5·6 관문 통과 증거.
- 스케줄러발 트레이스: span name `scheduler.*` 검색.
- 에러 경로: `{ status = error }` 로 chaos 채록분 검색.

## 7. 측정 수단 분담 — "요청 몇 개로 쟀냐"가 아니라 "시나리오별 담당 도구가 뭐냐"

Postman 컬렉션 하나로 전부 재는 게 아니다. 진입점 × 시나리오별로 담당 도구가 다르고, 컬렉션은 그중 첫 줄만 맡는다:

| 진입점 / 시나리오 | 측정 수단 | 커버 범위 | 상태 |
|---|---|---|---|
| HTTP 발 · 정상 흐름 | Postman 컬렉션 (요청 1~4, 6) | §1의 1~9 전 경계 — 댓글 E2E가 최다 통과 경로 | ✅ 즉시 실행 가능 |
| HTTP 발 · 핸들된 예외 (4xx) | Postman 요청 5 | 예외 경로에서도 trace가 **남는다**까지만. 핸들된 예외는 outcome=CLIENT_ERROR로 기록되고 error 태그는 안 붙을 수 있음 | ⚠️ error 태그 검증 아님 |
| 미처리 예외 · 슬로우 쿼리 (5xx, error 태그) | chaos 문항 2 (장애 주입) | §4-1 — span error 태그·exception 기록 | ⚠️ chaos 채록 시 |
| 스케줄러발 (비-HTTP 진입점) | 크론 실행 시각에 Tempo에서 `scheduler.*` span 검색 — HTTP 트리거가 없으므로 Postman 소관 밖 | §3 — 단 `@Observed` 없는 2개는 보강 선행 | ⚠️ 보강 후 확인 |
| 컨슈머 실패 · 재시도 · DLQ | chaos 컨슈머 실패 문항 | §4-2 | ⚠️ chaos 채록 시 |
| 캐시 미스 시 content→user client span | Postman 요청 3 — 단 TTL 내 재실행은 히트만 탄다. 미스 확인은 TTL 만료 후 첫 호출 | §1-7 | ⚠️ 미스 케이스 별도 1회 |

**왜 컬렉션에 API를 더 안 넣나**: battle 댓글, product 리뷰 등 다른 API를 추가해도 같은 계측 스위치(JDBC/Kafka/Redis는 전역)를 지나므로 커버리지 정보가 늘지 않는다. 요청 수를 늘리는 건 다시 샘플링 사고로 돌아가는 것이고, 늘려야 하는 건 **아직 안 지나본 경계·시나리오**(위 표의 ⚠️ 줄)다.

> 공격 대응 요약: "6개 요청으로 쟀냐"는 질문에는 "요청 6개가 아니라 경계 10종 × 진입점 3종(HTTP / 스케줄러 / 컨슈머) × 정상·비정상 흐름의 매트릭스이고, HTTP 발 정상 흐름은 Postman, 스케줄러발은 크론 시각 Tempo 검색, 실패 경로는 장애 주입으로 분담했다"가 답이다. 이 표가 그 매트릭스다.

## 남은 액션 (chaos 문항 채록과 연동)

- [ ] toy-chat 배포 후 댓글 E2E 워터폴 1회 → §1의 6~9 일괄 실측 (Step 0 종결과 동일 검증)
- [ ] `FeedTrendingScheduler`·`ProductPopularityScheduler` `@Observed` 추가 → 스케줄러 root span 사각지대 해소
- [ ] chaos 문항 2 채록 시 error 태그 확인 → §4-1 종결
- [ ] chaos 컨슈머 실패 문항 채록 시 DLQ 트레이스 확인 → §4-2 종결
- [ ] 문항별 traceId 채록 체크리스트에 "이 트레이스에 예상 span이 다 있나" 항목 추가
