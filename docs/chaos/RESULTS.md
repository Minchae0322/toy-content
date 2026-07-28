# 장애 주입 결과 종합

문항별로 "무엇을 주입했고, 시스템이 실제로 어떻게 동작했고, 무엇을 발견·수정·검증했는지"를 한곳에 모은다. 각 주장 옆 링크가 원본 근거(리포트·finding·evidence)다. 정식 블라인드 채점(§8)은 이 문서와 별개로 answer.md 앵커 기준으로 진행한다.

---

## CH-1 — MongoDB 다운 (알림 저장 경로)

### 한 줄 요약

Mongo 다운 문항 하나를 4회 주입해서 — 실서비스 잠복 버그 2건과 계측·탐지 갭 5건을 찾았고, 핵심 버그(컨슈머 예외 삼킴 → 조용한 유실)를 수정·배포한 뒤 재주입으로 **유실 0을 실측 검증**했으며, AI RCA 에이전트 블라인드 조사 2회로 "계측이 자백하면 원인 확정, 침묵하면 가설 정지"라는 대조 데이터까지 얻었다.

### 실행 이력 (2026-07-25 ~ 07-26, 주입 4회)

| 회차 | 일시(UTC) | 다운 시간 | 결과 | 판정 |
|---|---|---|---|---|
| 0 | 07-25 13:59 | 16초 | 증상 없음 — 측정 라벨 버그 + 트리거가 백엔드 미도달(CDN 404 마스킹) + 대기 부족의 3중 무효 | 무효 (도구 결함 발견의 원천) |
| 1 | 07-26 07:53 | 73초 | 알림 **24.7초 지연** 도착, 유실 0 — 드라이버 대기(30s) 안에서 흡수 | 유효 (경계 안쪽) |
| 2 | 07-26 08:13 | 4분 59초 | 무효 — 트리거를 원복 3초 전에 발사, 노출 부족으로 또 지연 흡수 | 무효 (타이밍 프로토콜 교훈) |
| 3 | 07-26 08:20 | 4분 31초 | **재시도 4회(30.0s×4) → DLQ → 복구 3초 후 재처리 성공** — 총 3분 36초 지연, 유실 0 | 유효 (경계 바깥) |

### 실측으로 확정된 시스템 동작

- **지연↔유실의 경계는 Mongo 드라이버 `serverSelectionTimeoutMS`(기본 30s) 하나다.** 다운이 그 안에 끝나면 컨슈머 스레드가 블로킹된 채 복구를 기다려 지연으로만 나타난다(회차 1: 트레이스에 자식 span 없는 23.44초 공백). 넘어가면 예외가 터진다(회차 3: 정확히 30.0초짜리 실패 4회).
- **수정 전 코드였다면 회차 3은 영구 유실이었다.** 예외 삼킴 제거(toy-chat `5eecb0a`, 07-25 배포) 덕분에 재시도(1s×3)→DLQ 발행→재처리 리스너 1분 백오프→복구 직후 성공으로 이어졌다. 장애 주입이 수정의 회귀 검증까지 해준 셈.
- **이 장애는 (수정 전 기준) 기존 알람 어디에도 안 걸린다.** 트리거 요청은 200, consumer lag 0, 5xx 없음, DLQ 없음 — 남는 신호는 ERROR 로그 한 줄과 `mongodb_up=0`뿐. "조용한 장애"의 실물 표본.
- 상세 타임라인 표: [rca-agent NF-07](../../../yogurtte-rca-agent/docs/findings/nf-07-notification-delay-loss-boundary.md)

### 이 문항이 찾아낸 결함·갭

| # | 발견 | 위치 | 상태 |
|---|---|---|---|
| 1 | 컨슈머 예외 삼킴 → 재시도/DLQ 데드코드, 조용한 유실 | toy-chat | **수정·배포·재주입 검증 완료** (`5eecb0a`, 회차 3) |
| 2 | CDN이 API 404를 SPA index.html 200으로 마스킹 — 클라이언트 오류 처리 전반이 깨짐, 상태코드 기반 게이트 전부 무력화 | 인프라(공인 경로) | 미수정 — observability.md 다음 단계 |
| 3 | Loki 라벨 스키마 이원화: 로그는 `service_name`, 메트릭만 `application` — `{application=...}` LogQL은 전부 빈 결과 | 계측/도구 | chaos.sh·RUNBOOK 수정 완료 / rca-agent 미수정 |
| 4 | DLQ 발행→재처리 사이 trace 단절(`traceId=NONE`) — "유실"과 "복구 도착"을 관측으로 구분 불가 | toy-chat 계측 | 미수정 — [NF-08](../../../yogurtte-rca-agent/docs/findings/nf-08-dlq-trace-discontinuity.md) |
| 5 | 드라이버 대기 30초 구간이 계측 사각(span 없는 공백) — 지연형 장애의 원인 확정 불가 | toy-chat 계측 | 미수정 (`mongodb.driver.*` 메트릭 or checkout span 필요) |
| 6 | 자동 탐지 갭 — `알림 처리 실패` 로그 알람·`mongodb_up` 알람·`DLQ publish rate>0` 알람 부재 | 알림 룰 | 미수정 |
| 7 | 장애 중 chat pod 메트릭 스크레이프 공백(2회 재현) | 관측 파이프라인 | 미해명 |

### AI RCA(rca-agent v0) 블라인드 조사 — 2회 비교

같은 모델·같은 프롬프트, 입력은 "증상 창 트리거 요청의 traceId + 제보 문장" 하나씩.

| | 회차 1 조사 | 회차 3 조사 |
|---|---|---|
| 질문 | "왜 알림이 늦었어?" | "왜 알림이 안 왔어?" |
| tokens | in 44,798 / out 9,162 | in 47,503 / out 6,756 |
| 비용 · 시간 | $1.0845 · 135.4s | $1.0200 · 101.8s |
| 원인 도달 | 위치만 특정(23.44s 공백), 원인은 "커넥션/서버 셀렉션 대기" **가설·확신도 낮음**에서 정지 | **"MongoDB 다운(mongod 미리스닝)" 확신도 높음** — 정답 |
| 근거 경로 백미 | lag(핸드오프 1.45ms)·풀(pending 0)·GC(0.0028s/s)를 수치로 배제 | 30.0s×4 = 드라이버 기본값 대조, refused vs timeout 구분, **같은 호스트 Redis 0.5ms 정상을 반증으로** 호스트 장애 배제 |
| 오귀인 | 없음 | 없음 |
| 오판 | — | 영향을 "유실"로 판정 (실제: DLQ 복구 도착) — 원인은 결함 #4 trace 단절 + 에이전트 자체 Loki 셀렉터 버그 |
| 기록 | [리포트](../../../yogurtte-rca-agent/reports/) · [AE-01](../../../yogurtte-rca-agent/docs/findings/ae-01-rca-v0-ch1-blind-eval.md) | [리포트](../../../yogurtte-rca-agent/reports/) · [AE-02](../../../yogurtte-rca-agent/docs/findings/ae-02-rca-v0-ch1-round2-eval.md) |

**두 조사의 대조가 이 실험의 핵심 결론이다**: 회차 1과 3 사이에 달라진 건 모델이 아니라 계측(`cdca2a5` Mongo 명령 트레이싱 + `5eecb0a` 에러 전파)뿐이다. trace가 error span과 예외 원문을 실어주자 v0 단발 호출로도 원인이 확정됐다 — **AI 모니터링의 상한은 모델이 아니라 계측이 정한다.**

### 포트폴리오 포인트

- **풀 사이클 스토리**: 장애 주입 → 도구·시스템 버그 발견 → 수정 배포 → 재주입으로 회귀 검증 → AI RCA 평가. 각 단계가 커밋·리포트·실측 수치로 증빙된다.
- **숫자로 말할 수 있는 것들**: 30.0초×4회(설정값이 트레이스에 그대로 보임), 지연 24.7초(경계 안) vs 3분 36초(경계 밖 DLQ 경유), 조사 1회 ≈ $1·2분, 유실 0(수정 후).
- **실패 회차도 소재다**: 회차 0의 3중 무효는 "측정이 틀리면 실험 전체가 조용히 무효가 된다"의 실례고, 그 대응으로 게이트(응답 JSON 검사, 라벨 검증)를 스크립트에 박았다. 회차 2는 타이밍 프로토콜(트리거 후 대기)이 문서가 아니라 근육이어야 하는 이유.
- **면접 단골 주제와의 접점**: at-least-once와 ack 시점, DLQ 설계와 재처리 백오프, 드라이버 타임아웃이 만드는 장애 등급의 절벽, trace context 전파의 경계(비동기·DLQ), 시그널별 라벨 스키마 불일치.

### 문서·증거 지도

| 무엇 | 어디 |
|---|---|
| 문항 정의·주입 방법·판정 기준 | [RUNBOOK.md](RUNBOOK.md) CH-1 절, [COMMANDS.md](COMMANDS.md) |
| 정답지·채점 앵커 | [scenarios/CH-1/answer.md](scenarios/CH-1/answer.md) |
| 채록 원본(evidence JSON·timeline.log) | master `~/chaos/scenarios/CH-1/evidence/` (rsync로 회수) |
| **회차별 상세 기록** (장애 상황·신호 발췌·원인 대조·스샷용 traceId) | rca-agent `docs/ch-1/round-1.md`, `round-3.md` |
| 에이전트 분석 원문 + 토큰·비용 | rca-agent `reports/6a65bd43...md`(회차 1) · `reports/6a65c38b...md`(회차 3) |
| 실행별 평가 | rca-agent `docs/findings/ae-01`, `ae-02` |
| 시스템 발견 상세 | rca-agent `docs/findings/nf-07`(경계 실측), `nf-08`(trace 단절) |
| 라벨 스키마·CDN 마스킹 발견 경위 | [observability.md](../observability/observability.md) 2026-07-25 절 |

---

## IN-2 — Kafka 다운 (알림 발행 경로, 조용한 유실)

### 한 줄 요약

Kafka를 5분 17초 내렸더니 — 댓글 API는 끝까지 200이었고 알림은 정확히 1건(트리거분)만 조용히 영구 유실됐다. RUNBOOK이 파둔 함정("sendSafely가 예외를 삼켜 아무 데도 안 남는다")은 불성립 — 계측이 60,060ms error span과 traceId 실린 ERROR 로그로 자백했다. 대신 소비자(chat)는 다운 내내 로그 0건으로 침묵했고, 그 원인을 캐다 **chat이 프로덕션에서 dev 프로필로 떠 있는 것**까지 발견했다.

### 실행 이력 (2026-07-26, 주입 1회 — §8.1 반복은 다음 회차)

| 회차 | 일시(UTC) | 다운 시간 | 결과 | 판정 |
|---|---|---|---|---|
| 1 | 07-26 09:14 | 5분 17초 | 트리거 200 + 알림 1건 영구 유실(실사용자 피해 0건 실측), error span·ERROR 로그 도달 | 유효 |

타임라인: 09:12:40 baseline T1 → 알림 0.9초 도착(Mongo `createdAt` 대조) / 09:14:23 `docker stop kafka` / 09:15:36 symptom T1 → **200**(54ms) / 09:16:37.4 content `알림 발행 실패` ERROR(트리거 정확히 +60.0s) / 09:19:41 원복 / 09:21:09 복귀 T1 → 알림 1.5초 도착, `kafka_brokers=1` 복귀.

### 실측으로 확정된 시스템 동작

- **실패 지점이 예측과 달랐다.** 예측은 "send()는 비동기라 즉시 리턴, delivery.timeout(120s) 후 whenComplete 콜백에서만 실패 → span 무결"이었는데, 실제로는 브로커 완전 다운 시 send()가 **메타데이터 fetch에서 동기 블로킹**하다 `max.block.ms`(기본 60s) 만료로 호출 스레드에 던졌다 — future가 만들어지기도 전이라 whenComplete의 `[Kafka]` 로그는 안 찍히고, 리스너 catch의 `[Notification]` 로그와 span error가 남았다. 60.0s라는 숫자가 트레이스에 그대로 보이는 건 CH-1의 30.0s(serverSelectionTimeoutMS)와 같은 패턴 — **설정값이 시그널에 자수한다**.
- **CH-1과의 대비쌍이 실측으로 완성됐다.** 같은 "알림 안 와요"인데 — 소비측 장애(CH-1 Mongo)는 재시도→DLQ→복구 도착으로 유실 0, 발행측 장애(IN-2 Kafka)는 재시도 경로 자체가 없어 유실 1·복구 불가. 실패 지점이 ack 경계의 어느 쪽이냐가 장애 등급을 가른다.
- **신호의 방향도 역전된다.** CH-1은 소비자가 시끄럽고(재시도·DLQ 로그, error span) 발행자 무결 — IN-2는 발행자가 시끄럽고(producer NetworkClient WARN 스팸 + ERROR 1건) 소비자 완전 침묵. "어느 서비스 로그가 우는가"만으로 발행측/소비측 장애를 가를 수 있다는 뜻이자, 침묵하는 쪽만 보면 원인 도달이 불가능하다는 뜻.
- **유실은 발행 실패 로그 건수로 셀 수 있었다.** 주입 창 내 `알림 발행 실패` 1건 = Mongo 창 내 신규 문서 0건과 교차 일치 — 트리거분 1건 유실, 실사용자 피해 0건.

### 이 문항이 찾아낸 결함·갭

| # | 발견 | 위치 | 상태 |
|---|---|---|---|
| 1 | **chat이 프로덕션에서 `SPRING_PROFILES_ACTIVE=dev`로 기동** — config drift. Mongo observability DEBUG 덤프가 Loki로 홍수(로그 볼륨·검색 오염), dev 설정 전반이 프로덕션에 적용 중 | toy-chat 배포(매니페스트) | 미수정 |
| 2 | 소비자는 브로커 다운에 완전 침묵 — `org.apache.kafka=ERROR` 억제(dev·prod 공통)로 NetworkClient WARN이 안 남는다. 소비자 로그만으론 브로커 다운 탐지 불가 | toy-chat 로깅 | 미수정 — 레벨을 올리면 초당 수 건 스팸이라, 로그가 아니라 메트릭 알람(#3)이 정공법 |
| 3 | `kafka_brokers`는 다운 시 0이 아니라 **부재(absent)** — kafka-exporter가 브로커에 못 붙으면 메트릭 자체를 안 내보낸다. 값 기반 알람은 안 걸리고 `absent()` 계열이 필요 | 알림 룰 | 미수정 |
| 4 | `알림 발행 실패` ERROR 로그 알람 부재 — 유실이 로그 1줄로만 남는데 아무도 안 깨운다 (CH-1 #6과 같은 계열) | 알림 룰 | 미수정 |
| 5 | 잠복 위험(이번엔 미발화): max.block 60s 동기 블로킹 × `notificationExecutor`(core 2/max 5/queue 100/**CallerRunsPolicy**) — 브로커 다운 중 지속 발행 시 105건 초과부터 **HTTP 요청 스레드가 60초씩 블로킹**된다. "비동기라 안전"이 포화 시점에 무너지는 구조 | toy-content `AsyncConfig` | 미수정 |
| 6 | (긍정 판정) 함정 불성립 — 현행 `NotificationEventListener`가 span error + traceId ERROR 로그를 남겨 **계측 자백이 성립**한다. RUNBOOK §6 IN-2의 "sendSafely 예외 삼킴" 우려는 리스너 리팩토링으로 이미 닫혀 있었음 | toy-content | 검증 완료 |
| 7 | (부수) kafka-exporter가 `kafka_consumergroup_lag`를 이미 노출 중 — **보류 중인 CH-2의 전제(§10 앱 lag 메트릭)를 exporter 게이트로 대체 가능**할 수 있다 | CH-2 문항 설계 | 채택(2026-07-26) — CH-2 게이트를 `notification-processors`/`user.notifications` exporter 쿼리로 교체 |
| 8 | (부수) infra Mongo 컨테이너 무인증 접근 가능(VPC 내부 한정) — 이번 채록의 ground truth(`user_notifications` 직접 조회)로는 유용했지만 보안 항목 | 인프라 | 미수정 |

### AI RCA(rca-agent) 블라인드 조사

실시 완료 (2026-07-26, traceId `6a65d039...` + "왜 알림이 안 왔어?", **$0.92 · in 39,202/out 8,266 tok · 123.4s**):

- **정답**: 직접 원인("발행 실패 → chat 미도달") 확정, **유실 판정 정답**("이미 유실, 재시도·아웃박스 없음") — CH-1 회차 3의 유실 오판과 대칭으로, 이번엔 체인의 진짜 끊김이 trace에 있어서 맞혔다. 60,060ms = `max.block.ms` 대조, outbox 권고까지 우리 결론과 일치.
- **실패**: 하위 원인 1위를 "토픽 부재"로 오선정 (실제 = 2위 "브로커 다운"). `peer.service`의 클러스터 ID(다운 전 캐시)를 "연결 성립" 증거로 오독 + 브로커 측 데이터 전무(`kafka_brokers` 미수집·Loki 셀렉터 결함)가 원인 — **결함 #3(absent 알람)과 같은 뿌리**: 부재 신호를 못 쓴다.
- 상세: rca-agent `docs/findings/ae-03`, 회차 기록 `docs/in-2/round-1.md`

### 포트폴리오 포인트

- **대비쌍의 실측 완성**: CH-1(소비측, 유실 0·DLQ 복구)과 IN-2(발행측, 유실 1·복구 불가)가 같은 증상("알림 안 와요")에서 갈리는 지점을 커밋·트레이스·Mongo 문서 수준으로 증빙 — at-least-once의 보장 범위가 "ack 경계 안쪽"뿐임을 실물로 보여준다.
- **숫자로 말할 수 있는 것들**: 60.0s(max.block.ms가 트레이스에 자수), 다운 5분 17초, 유실 정확히 1건·실사용자 0건, E2E 알림 지연 0.9s(정상)→∞(유실)→1.5s(복구), producer WARN 547건/15m vs 소비자 0건.
- **예측→반증 스토리**: 코드 읽고 세운 가설(비동기 whenComplete 120s 실패, span 무결)이 실측(동기 max.block 60s 실패, span error)으로 뒤집혔다 — "코드 리뷰만으론 장애 양상을 못 맞춘다, 그래서 주입한다"의 실례.
- **문항이 문항 밖 결함을 낚았다**: 소비자 침묵의 원인 추적이 dev 프로필 발견(#1)으로 이어짐 — 장애 주입이 config drift 검출기 역할까지 한 사례.

### 문서·증거 지도

| 무엇 | 어디 |
|---|---|
| 문항 정의·주입 방법·판정 기준 | [RUNBOOK.md](RUNBOOK.md) IN-2 절, [COMMANDS.md](COMMANDS.md) |
| 정답지·채점 앵커·실측 도달 경로 | [scenarios/IN-2/answer.md](scenarios/IN-2/answer.md) |
| 채록 원본 | `scenarios/IN-2/evidence/` (레포로 회수 완료 — baseline 2회분·symptom·timeline.log) |
| 증상 트레이스 원본(60,060ms error span) | `scenarios/IN-2/evidence/symptom/20260726T091536Z/trace-symptom-t1.json` |
| **회차별 상세 기록** (원인 대조·스샷용 traceId·RCA 보고서) | rca-agent `docs/in-2/round-1.md` |
| 관측성 갭 상세 | [observability.md](../observability/observability.md) 2026-07-26 IN-2 절 |

---

## CH-2 · AU · IN-1 · IN-3 · AP 계열

미실행. 실행 시 위와 같은 골격(실행 이력 → 실측 확정 → 발견 → RCA 평가 → 포인트)으로 이어서 기록한다. CH-2는 exporter lag 메트릭(#7)으로 전제 충족을 확정했다(2026-07-26) — 게이트·런북 쿼리 교체 완료, 실행 가능.
