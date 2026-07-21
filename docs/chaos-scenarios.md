# Chaos 문항 설계 — chat · auth · 인프라 편

> AI 기반 RCA 채점용 장애 시나리오(문항) 중 content 단독으로 못 만드는 것들의 설계.
> 각 문항 = 주입 절차 + 정답지(원인 1줄) + 근거 시그널 + 복구 절차.
> 커버리지 미검증 3개([tracing-coverage.md](tracing-coverage.md) §4)를 문항 채록에 얹어 함께 닫는다.
> 마지막 갱신: 2026-07-21.

## 설계 원칙

1. **주입은 실제 장애와 같은 텔레메트리를 남긴다** — Thread.sleep 심는 합성 chaos 코드 지양. 인프라 레벨 주입(docker stop / kubectl scale / 리소스 limit)을 우선하고, 코드 변경은 "계측 구멍 메우기"에만 쓴다.
2. **정답 시그널이 관측 가능한지 먼저 확인** — 시그널이 안 잡히는 문항은 AI가 못 맞추는 게 아니라 문항이 성립 안 하는 것. 사전 작업(§3)이 그 전제 조건이다.
3. **증상→원인 거리(hop)를 다양하게** — 1-hop(AU-3)부터 다중 서비스 복합(IN-1)까지 섞어야 변별력 있는 문항 세트가 된다.

## 1. 문항 카탈로그

| ID | 시나리오 (정답지) | 주입 | hop | 전제 작업 |
|---|---|---|---|---|
| CH-1 | MongoDB 다운 → chat 컨슈머 실패 → 재시도 3회 → DLQ | infra 노드 `docker stop <mongo>` | 2 | Step 0 배포 검증 |
| CH-2 | chat 다운 → 컨슈머 lag 누적 → 복구 후 알림 몰아서 도착 | `kubectl scale deploy/chat-service --replicas=0` | 2 | **lag 메트릭 (§3-chat)** |
| AU-1 | auth CPU limit 과소 → 응답 지연 → content 캐시 미스 시 +3s / fallback | auth Deployment `resources.limits.cpu: 50m` | 2~3 | (권장) auth JDBC 계측 |
| AU-2 | auth 완전 다운 → 로그인 502, content는 "정상인데 전부 익명 사용자" | `kubectl scale deploy/auth-service --replicas=0` | 2 | 없음 |
| AU-3 | JWT 시크릿 드리프트 → content 전 인증 API 401 폭증 (로그인은 성공) | content Secret의 `JWT_SECRET` 변경 + restart | 1 | 없음 |
| IN-1 | Redis 다운 → 3개 서비스 동시 이상 (캐시·ShedLock·온라인조회·이메일인증) | infra 노드 `docker stop <redis>` | 다중 | 스케줄러 `@Observed` 보강 |
| IN-2 | Kafka 다운 → 댓글은 성공하는데 알림 조용히 유실 + 채팅 발신 실패 | infra 노드 `docker stop <kafka>` | 2 | 없음 (§4-1 error 태그 검증 겸용) |
| IN-3 | 커넥션 풀 고갈 → pending 적체 → 전면 지연 | k6 부하 + 슬로우 쿼리(기존 문항 2 조합) | 2 | Alert P0 룰 실구현 |
| IN-4 | Pod OOMKilled (보류) | heap 부하 | 2 | kube-state-metrics — 활성화 전까지 보류 |

### CH-1 — Mongo 다운 → 컨슈머 재시도 → DLQ

- **주입**: infra 노드에서 mongo 컨테이너 중지 → 댓글 1건 작성(Postman 컬렉션 요청 4).
- **기대 시그널**: 댓글 API는 200. chat consume span 이후 `notification.process`에서 Mongo insert 예외 → `DefaultErrorHandler`가 `FixedBackOff(1000ms, 3)` 재시도 → `DeadLetterPublishingRecoverer`가 DLQ 토픽 발행(producer span). 알림 미도착. 채팅 메시지 저장도 같이 실패(Mongo 공유)하므로 채팅 발신 에러가 부수 증상.
- **정답지**: "MongoDB 다운. 컨슈머는 3회 재시도 후 DLQ 적재 — 메시지 유실은 없음."
- **커버리지 연동**: §4-2(DLQ 트레이스 실측) + Mongo span error 태그 확인.
- **복구**: mongo 기동 → DLQ 적재분 처리 방침 확인(재발행 수단이 있는지 — 없으면 그 사실 자체를 정답지 비고에 기록).

### CH-2 — 컨슈머 정지 → lag 누적

- **주입**: chat replica 0 → 5분간 댓글/좋아요 트래픽 → replica 1 복구.
- **기대 시그널**: `websocket_active_users` 0으로 급락(기존 메트릭 — 2026-05-21 알람 후보 그대로), lag 급증 → 복구 후 알림이 몰아서 도착하며 해소. producer 쪽은 정상이므로 content 트레이스는 깨끗 — "발행은 됐는데 소비가 없다"를 lag로 지목하는 문항.
- **전제**: 현재 chat에 컨슈머 lag 메트릭이 **전무**(§3). 이거 없으면 정답 시그널이 관측 불가라 문항 성립 안 함.

### AU-1 — auth CPU 기아 → 조용한 성능·데이터 품질 저하

- **주입**: auth Deployment에 `resources.limits.cpu: 50m` 적용(kubectl patch — GitOps 매니페스트와 drift 생기므로 채록 후 즉시 원복).
- **기대 시그널**: 로그인 P99 급등. content는 5xx 없이 정상처럼 보이나 — 캐시 TTL 만료 후 첫 조회에서 `ExternalUserApiClient`의 3s timeout(`.timeout(TIMEOUT)`) → `onErrorReturn(fallback)` → **피드에 익명(fallback) 사용자 표시**. content 트레이스에서 `GET user-service` client span이 3s에서 잘림.
- **정답지**: "auth CPU limit 과소 → 응답 지연. content는 fallback으로 방어했으나 데이터 품질 저하."
- **변별 포인트**: 에러율은 안 올라가는 장애 — 메트릭만 보면 놓치고, trace의 client span + fallback 로그로 찾아야 함.

### AU-2 — auth 완전 다운

- **기대 시그널**: 로그인 5xx(ingress 502), content는 캐시 히트분은 멀쩡 + 미스분은 익명 사용자. AU-1과 증상은 비슷하되 client span이 timeout이 아니라 connection refused로 즉시 실패 — 둘을 구별하는 것 자체가 채점 포인트.

### AU-3 — JWT 시크릿 드리프트 (config drift)

- **주입**: content 쪽 `JWT_SECRET`만 다른 값으로 변경 + rolling restart.
- **기대 시그널**: 로그인(auth)은 성공하는데 content 전 인증 API가 401 — "로그인은 되는데 아무것도 안 됨". 401은 4xx라 **error 태그가 안 붙는다**(tracing-coverage §7) — 메트릭(4xx rate)과 JwtFilter 로그로 찾는 문항. trace 의존도가 낮은 문항을 하나 섞는 목적도 있음.
- **복구**: Secret 원복 + restart. 실사용자 전원 영향이므로 저트래픽 시간대 필수.

### IN-1 — Redis 다운 (다중 서비스 복합, 최고 난도)

- **기대 시그널**: ① content — user 캐시 조회 실패 → auth 직행 호출 급증·latency 상승, ShedLock 획득 실패 → 스케줄러 skip → **수 분 뒤 핫스코어 갱신 정체**(2차 지연 증상). ② chat — 온라인 디바이스 조회 실패 → WS/FCM 발송 이상. ③ auth — 이메일 인증·위치 기능 실패. 세 서비스가 동시에 다르게 아픈 걸 단일 근원으로 수렴시키는 문항.
- **채록 전 확인**: content `UserCacheStore`가 Redis 예외를 삼키고 API 직행으로 넘어가는지, 아니면 요청까지 깨지는지 — 실동작이 정답지의 일부.
- **전제**: `FeedTrendingScheduler`·`ProductPopularityScheduler` `@Observed` 보강(기존 보강 항목) — 없으면 스케줄러 증상이 트레이스에 안 남는다.

### IN-2 — Kafka 다운 (조용한 유실)

- **기대 시그널**: 댓글 200 성공(발행은 AFTER_COMMIT 뒤) + `NotificationService.sendSafely`가 예외를 삼키므로 **알림만 조용히 유실**. producer span의 error 태그가 남는지가 §4-1(에러 경로 error 태그) 검증과 직결. chat의 채팅 발행 실패는 사용자 가시 증상.
- **주의**: 유실된 알림은 복구 불가(재시도 없음) — 데이터 유실을 동반하는 문항임을 인지하고 진행. "sendSafely가 삼켜서 아무 데도 안 보인다"가 결과라면 그 자체가 계측 구멍 발견 → 보강 후 재채록.

### IN-3 — 커넥션 풀 고갈

- **주입**: 기존 문항 2(슬로우 쿼리)에 k6 부하를 얹으면 자연 발생 — 별도 주입 불필요.
- **기대 시그널**: `hikaricp_connections_pending > 0` → P0 알람 발화(2026-05-22 체크리스트의 "실 발화 테스트" 항목을 이 문항으로 소화). trace는 connection acquire 대기가 JDBC span 앞단 공백으로 보임.

## 2. 레포별 사전 작업

| 레포 | 작업 | 근거 / 필요한 문항 | 크기 |
|---|---|---|---|
| toy-chat | (코드 완료) `cab6741` 배포 후 Step 0 워터폴 검증 | 모든 chat 문항의 전제 — consume span 없으면 정답지가 안 만들어짐 | 검증만 |
| toy-chat | `ConsumerFactory`에 `MicrometerConsumerListener` 등록 → `kafka_consumer_fetch_manager_records_lag*` 노출 + Grafana lag 패널 1개 | CH-2 정답 시그널. 05-21 다음 단계의 "컨슈머 lag 메트릭" 항목과 동일 | ~5줄 |
| toy-auth | `datasource-micrometer-spring-boot:1.1.1` 추가 (content/chat과 동일 버전) — **auth는 현재 JDBC span 전무** | AU-1/2에서 auth 내부 원인 분해. 없으면 auth server span이 원인 불명의 통짜 지연으로만 보임 | 의존성 1줄 |
| toy-auth | (선택) Lettuce `MicrometerTracing` — auth도 Redis 사용(이메일 인증·위치) | IN-1에서 auth 증상 분해. v1에서는 로그로 충분하면 보류 | ~10줄 |
| toy-content | `FeedTrendingScheduler`·`ProductPopularityScheduler` `@Observed` (기존 보강 항목) | IN-1 스케줄러 증상 관측 | 2줄 |
| 인프라 | infra 노드 docker-compose **서비스명·중지/기동 명령 runbook** 확인 (mongo/redis/kafka) | CH-1, IN-1, IN-2 주입 수단 | 문서 |
| 인프라 | Grafana Alert 룰 실구현 (P0 HikariCP → 5xx → P99 순, 05-22 계획분) | 전 문항에서 "알람이 울렸는가"도 채록 대상 — RCA 문항의 가치 상승 | 대시보드 |
| 인프라 | (보류) kube-state-metrics 활성화 | IN-4(OOMKilled), AU-1의 CPU throttling 메트릭 교차 검증 | chart 설정 |

exporter(redis/kafka/mongodb-exporter)는 v1에서 **불요** — 앱 쪽 시그널(예외·span·메트릭)만으로 전 문항의 정답 도달이 가능하다. 인프라 쪽 메트릭이 없어서 문항이 성립 안 하는 건 CH-2(lag)뿐이고, 그건 앱 쪽 5줄로 해결.

## 2.5 주입 토글화 — "카오스용/정상용 옵션" 검토 결과

**앱 레벨 옵션(프로필/플래그, chaos-monkey-spring-boot)은 배제한다.** 이유: 주입 방식이 텔레메트리 모양을 바꾼다. 예컨대 CM4SB latency assault를 `ExternalUserApiClient`에 걸면 sleep이 메서드 앞에 끼므로 HTTP client span은 여전히 빠르고 server span에 원인 불명 공백만 생긴다 — AU-1 정답지(3s에서 잘리는 client span)와 다른 트레이스. RCA 문항은 정답 근거가 텔레메트리 모양 그 자체라, 흉내 낸 장애는 모양이 다르면 문항으로 무효. Kafka 다운을 예외 assault로 흉내 내도 connection refused 스택·타 서비스 동시 증상·복구 후 lag 해소 같은 진짜 장애의 지문이 안 생긴다.

**대신 토글은 운영 계층에서 만든다** — 문항별 on/off 스크립트(`chaos.sh <문항ID> on|off`). 각 문항의 주입이 전부 3~4줄 kubectl/docker 명령이므로 래핑만 하면 되고, `off`(복구)가 항상 코드로 존재해 안전 수칙 1번이 구조적으로 충족된다. 별도 카오스 환경(스테이징 복제)은 EC2 3대 규모에서 비현실적 + 정답지 재료가 실환경 텔레메트리라 목적에도 부적합.

CM4SB는 추후 "임의 서비스 빈 예외" 같은 순수 앱 결함 문항을 늘릴 때 프로필 gate로 재검토.

## 3. 문항 공통 채록 절차

1. **baseline** — 대시보드 정상 상태 스크린샷, 주입 전 대표 traceId 1개.
2. **주입** — 시각(UTC) 기록. 한 번에 한 문항만.
3. **트리거** — Postman 컬렉션 또는 k6로 증상 재현 트래픽.
4. **채록** — 증상 traceId, 대시보드 스크린샷, 알람 발화 여부, Loki 쿼리 결과.
5. **복구 + 정상화 확인** — 복구 명령은 주입 **전에** 준비해둔다.
6. **정답지 작성** — 원인 1줄 + 근거 시그널 경로(어떤 순서로 보면 도달하는가).
7. **커버리지 체크** — "이 트레이스에 예상 span이 전부 있나" (tracing-coverage §4 연동).

## 4. 안전 수칙

- 이 클러스터가 곧 실서비스다 — 저트래픽 시간대, 문항당 주입 시간 최소화(수 분), 복구 검증까지가 한 문항.
- IN-2(Kafka)·CH-1(Mongo)은 **데이터 유실/지연을 동반** — 알림 유실 허용 범위를 먼저 결정.
- kubectl 직접 변경(scale, patch)은 GitOps 매니페스트와 drift — 채록 끝나면 즉시 원복하고, 매니페스트 레포 기준 상태로 되돌아왔는지 확인.
- AU-3(시크릿 드리프트)은 전 사용자 영향 — 가장 마지막에, 가장 짧게.
