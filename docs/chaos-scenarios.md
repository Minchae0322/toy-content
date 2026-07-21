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
- kubectl 직접 변경(scale, patch)은 GitOps 매니페스트와 drift — 채록 끝나면 즉시 원복하고, 매니페스트 레포 기준 상태로 되돌아왔는지 확인. **단, §5의 레포 실사가 끝나기 전에는 이 수칙이 성립하지 않는다(레포 기준으로 원복하면 스켈레톤이 배포됨).**
- AU-3(시크릿 드리프트)은 전 사용자 영향 — 가장 마지막에, 가장 짧게.

## 5. 배포 매니페스트 실사 — 운영본 vs GitOps 레포 (2026-07-21)

master 노드의 heredoc 매니페스트(운영 적용본)와 `yogurtte-k8s-manifests` 레포를 비교한 결과. **레포는 초기 스켈레톤에서 멈춰 있고, 실제 운영 설정은 master 노드 홈 디렉토리의 파일에만 존재한다.** chaos 채록을 시작하기 전에 이 drift를 닫아야 하는 이유: ① 안전 수칙의 "레포 기준 원복"이 현재는 서비스를 죽이는 명령이고, ② ArgoCD ApplicationSet(automated + selfHeal + prune)이 실제로 동작 중이라면 AU-1 같은 kubectl patch 주입을 selfHeal이 수 초 내 되돌려 문항 자체가 성립하지 않는다.

### 서비스별 주요 차이

| 항목 | 레포 (base) | 운영본 (heredoc) | 영향 |
|---|---|---|---|
| content 프로브 | **8082 `/api/actuator/...`** | 8090 `/actuator/...` | 관리 포트 분리(05-21) 이전 상태. 레포본이 배포되면 프로브 실패 → CrashLoop |
| content replicas | 1 | 2 + podAntiAffinity + maxSurge 0 | 가용성 설계 전체가 레포에 없음 |
| content JVM/리소스 | Xmx1280m, limits 1792Mi | Xmx1024m, limits 1536Mi | 어느 쪽이 의도인지 결정 필요 |
| auth/user | `apps/auth`(image `auth:latest` — 존재하지 않는 이미지)와 `apps/user` **중복 존재**, 둘 다 8080 | `auth-service` 단일, 8081/8090, startupProbe | ApplicationSet이 둘 다 Application으로 생성 |
| chat | 스켈레톤(8080, 프로브 없음) | 8084/8090, firebase-key 볼륨, scrape 어노테이션 | 레포본에는 운영 설정 전무 |
| 공통 | scrape 어노테이션, TEMPO_ZIPKIN_ENDPOINT, imagePullSecrets(일부) 없음 | 전부 있음 | Alloy 수집·트레이싱이 레포본에는 없음 |
| 이미지 전략 | kustomize SHA 고정 (CI가 갱신) | `:latest` + `imagePullPolicy: Always` + rollout restart | 롤백 워크플로(latest 재태깅)는 운영 방식 기준. 두 전략이 공존 중 |
| content dev overlay | `images` 항목 2개(`content`→옛 SHA 고정, `ghcr.io/...`→CI 갱신) | — | 첫 항목이 옛 SHA로 얼어 있음. 어느 항목이 이기는지 kustomize 변환 순서에 의존 — 정리 필요 |

운영본 내부에서도 서비스 간 비대칭이 있다: startupProbe는 auth만 도입(chat은 liveness initialDelay 200s로 버팀), JVM 옵션은 auth가 `JAVA_TOOL_OPTIONS`·content가 `JAVA_OPTS`(Dockerfile ENTRYPOINT가 `java ${JAVA_OPTS} -jar`로 참조하므로 **둘 다 동작함** — content 매니페스트 주석의 "JAVA_OPTS를 안 읽을 수 있다" 우려는 해소, 컨벤션만 통일하면 됨).

### 정리 체크리스트

**Step 0 — 사실 확인 (master에서, 변경 없이)**
- [ ] `kubectl get pods -n argocd` — ArgoCD가 실제로 설치·동작 중인가
- [ ] `kubectl get deploy -A` — 서비스가 어느 네임스페이스에 떠 있나 (수동 적용본 vs ApplicationSet의 dev/prod)
- [ ] `kubectl get deploy content-service -o yaml`로 live 스펙 덤프 — heredoc 파일은 "의도"이고 live가 "사실". 셋(heredoc/live/레포)을 비교해 기준본 확정

**Step 1 — 소스 오브 트루스 정리 (chaos 채록 전 필수)**
- [ ] live 스펙을 `apps/*/base/deployment.yaml`로 역반영 (이름도 `content-service` 등 실제와 일치시킴)
- [ ] `apps/auth` 삭제, ApplicationSet elements에서 `auth` 제거 (user와 중복)
- [ ] content dev overlay의 이중 `images` 항목을 1개로 정리
- [ ] 이미지 전략 하나로 통일 — 권장: kustomize SHA 고정으로 수렴 (CI가 이미 갱신 중), 롤백은 latest 재태깅 대신 매니페스트 revert로 단순화
- [ ] prod overlay가 실체 없으면 ApplicationSet env 리스트를 dev만으로 축소
- [ ] 반영 후 ArgoCD sync 상태가 Healthy인지, 운영 파드가 교체되지 않았는지 확인

**Step 2 — 매니페스트 개선 (운영본 기준)**
- [ ] chat·content에 startupProbe 도입, liveness/readiness의 initialDelay(200s/150s/120s/90s) 제거 — auth에 이미 검증된 패턴. 현재는 배포마다 프로브 대기로 수 분 낭비 + 부팅이 느려지면 restart 루프 위험
- [ ] chat cpu limit 300m → 500m 검토 (auth와 같은 부팅 스로틀링 완화)
- [ ] JVM 옵션 키를 `JAVA_TOOL_OPTIONS`로 통일 (Dockerfile 의존 제거)
- [ ] auth 헤더 주석(limits 512Mi)과 실제 값(640Mi) 불일치 수정
- [ ] hostPath logs/dumps는 노드 종속 — 힙덤프 수거 시 "어느 노드의 pod였나" 확인 절차를 runbook에 명시

**Step 3 — chaos 주입과 GitOps의 공존**
- [ ] AU-1(kubectl patch) 주입 전 해당 Application의 auto-sync 일시 해제, 원복 후 재활성 — 이 두 명령을 `chaos.sh <ID> on|off`에 포함시켜 selfHeal과의 충돌을 구조적으로 차단
- [ ] `chaos.sh off`의 원복 기준을 "매니페스트 레포 상태"로 명시 (Step 1 완료가 전제)

## 6. 포트폴리오 패키징 — 문항을 테스트케이스로

### 구성

문항 하나 = 테스트케이스 하나로 패키징한다. 형식은 Given / When / Then:

```
docs/chaos/
  README.md              # 문항 카탈로그 표 + 결과 매트릭스 (아래 참조)
  scripts/chaos.sh       # <문항ID> on|off — 주입·원복이 항상 쌍으로 존재
  scenarios/AU-1/
    runbook.md           # Given: 전제 작업·baseline 상태 / When: 주입 + 트리거 트래픽
    answer.md            # Then: 정답지 1줄 + 근거 시그널 도달 경로 (어떤 순서로 보면 원인에 닿는가)
    evidence/            # baseline·증상 대시보드 스크린샷, 대표 traceId, Loki 쿼리, 알람 발화 캡처
```

README의 결과 매트릭스가 포트폴리오의 얼굴이다: 문항 | 주입 | 사용자 증상 | 근거 시그널 | 탐지 수단(알람/대시보드/트레이스) | 채록 결과 발견된 계측 구멍 → 보강 커밋 링크. 대표 문항 1~2개(AU-1, IN-1 추천 — 에러율이 안 오르는 장애와 다중 서비스 복합)는 트레이스 워터폴 스크린샷과 함께 딥다이브로 쓴다.

### 판단 근거

1. **주입 충실도** — §2.5에서 이미 논증: 앱 레벨 흉내(sleep, 예외 assault)는 텔레메트리 모양이 실제 장애와 달라 문항이 무효. 인프라 레벨 주입만 쓴다. 테스트케이스 관점에서도 같은 결론 — Then(기대 시그널)이 검증 대상인데 When이 가짜면 Then도 가짜다.
2. **재현 가능성이 테스트케이스의 자격** — 주입·원복·전제·트리거가 전부 코드/문서로 존재해야 "실험"이 아니라 "테스트"다. chaos.sh의 on/off 쌍, runbook의 Given이 그 역할. 같은 문항을 다시 돌리면 같은 시그널이 나와야 하고, 안 나오면 그게 회귀다.
3. **Then의 실패가 곧 성과** — 기대 시그널이 관측되지 않으면 문항 실패가 아니라 **계측 구멍 발견**이다(IN-2의 sendSafely 케이스). "장애를 주입해 관측성 스택을 테스트하고, 실패한 assertion마다 보강 커밋이 나왔다"가 이 작업의 포트폴리오 서사 — 시나리오 수보다 이 피드백 루프의 증거(발견→커밋 링크)가 설득력의 핵심.
4. **v1은 수동 채록** — 실서비스 클러스터라 자동 반복 실행은 위험 대비 이득이 없다. 자동화 여지는 Then 검증(PromQL/Tempo API로 기대 시그널 존재 확인하는 verify 스크립트)에만 남겨두고, 주입의 자동 스케줄링은 하지 않는다.
5. **공개 레포 주의** — evidence 스크린샷에 사설 IP·내부 호스트명이 들어가지 않게 캡처 범위를 정한다(대시보드 변수 영역 제외). 레포 공개 방침(2026-07 시크릿 정리)과 충돌하지 않게.
