# CH-1 실행 중 발견된 실버그 — 컨슈머가 예외를 삼켜 재시도/DLQ 무력화

> 시나리오 CH-1 (Mongo 다운 → 컨슈머 재시도 → DLQ) 을 실측하다 **문항의 전제(재시도→DLQ 격리, 유실 없음) 자체가 성립하지 않는 구조 결함** 을 발견. 카오스 훈련이 첫 판에 실버그를 잡은 케이스.
>
> 대상 코드: `toy-chat` — `chat-service` 의 `UserNotificationConsumer`

---

## 한 줄 요약

`UserNotificationConsumer` 가 모든 예외를 `catch` 하고 `ack.acknowledge()` 까지 호출하기 때문에, 설정돼 있는 재시도(`FixedBackOff 1s × 3`) 와 DLQ 발행에 **예외가 도달할 수 없다.** 결과: 에러 로그 한 줄 남기고 **조용한 유실**. DLQ 재처리 리스너는 완전한 데드코드.

---

## 설계(의도) vs 실제

### 의도된 흐름 — CH-1 답안이 전제하는 것

```
1. dto 처리 시도 → Mongo write → DataAccessException
2. 리스너 밖으로 예외 전파
3. DefaultErrorHandler 잡음
4. FixedBackOff(1000ms) → 1초 sleep → 재시도
5. 3회 모두 실패 시 DeadLetterPublishingRecoverer 가 user.notifications.dlq 로 발행
6. 오프셋 커밋
7. Mongo 복구 후 handleRetryNotification 또는 수동 재발행으로 복구
   → 유실 없음
```

### 실제 흐름 — 지금 코드가 하는 것

```
1. dto 처리 시도 → Mongo write → DataAccessException
2. handleNotification 의 catch 가 잡음
3. handleNotificationError 가 로그만 찍고 ack.acknowledge()
4. Spring Kafka 컨테이너: "성공 처리" 로 인식
5. DefaultErrorHandler 발동 X (재시도 X, DLQ X)
6. 오프셋 커밋 → 메시지 소멸
   → 알림 조용히 유실
```

---

## 근거 (코드)

### 재시도 + DLQ 는 실제로 설정돼 있다
`toy-chat` — `KafkaConsumerConfig.java:212-217` (`notificationListenerFactory`):

```java
DefaultErrorHandler errorHandler = new DefaultErrorHandler(
    deadLetterPublishingRecoverer(),
    new FixedBackOff(1000L, 3)          // 1초 간격 × 3회 재시도
);
errorHandler.addNotRetryableExceptions(SerializationException.class);
factory.setCommonErrorHandler(errorHandler);
```

- **재시도 횟수: 3회** (`FixedBackOff(1000L, 3)` 의 두 번째 인자가 maxAttempts. 최초 1회 + 재시도 3회 = **총 4회 시도**)
- **재시도 간격: 1초**
- **모두 실패 시**: `DeadLetterPublishingRecoverer` 가 `user.notifications.dlq` 로 발행

### 그런데 컨슈머가 예외를 삼킨다
`toy-chat` — `UserNotificationConsumer.java:40-53`:

```java
try {
  notificationService.processNotification(notificationDto);
  ack.acknowledge();
} catch (Exception e) {
  log.error("[Kafka] 알림 처리 실패: userId={}, type={}, error={}",
      notificationDto.getUserId(), notificationDto.getType(), e.getMessage(), e);
  handleNotificationError(notificationDto, e, ack);   // ← 예외 삼킴
}
```

### `handleNotificationError` 가 ack 까지 한다
`toy-chat` — `UserNotificationConsumer.java:58-69`:

```java
private void handleNotificationError(KafkaNotificationDto notificationDto, Exception error,
    Acknowledgment ack) {
  try {
    log.error("[Kafka] 알림 처리 오류: userId={}, type={}, error={}",
        notificationDto.getUserId(), notificationDto.getType(), error.getMessage());
    ack.acknowledge();                    // ← 오프셋 커밋
  } catch (Exception handlingError) {
    log.error("[Kafka] 에러 핸들링 중 추가 오류: userId={}, handlingError={}",
        notificationDto.getUserId(), handlingError.getMessage());
    ack.acknowledge();
  }
}
```

`DefaultErrorHandler` 는 **리스너 메서드가 예외를 던져야 발동** 하는데, 여기서 예외를 완전히 흡수하고 오프셋까지 커밋 → Spring Kafka 는 "성공한 메시지" 로 인식 → 재시도/DLQ 경로 자체가 실행되지 않음.

---

## 왜 Mongo 오류가 Kafka 흐름과 엮이는가

Kafka 컨슈머의 재시도/DLQ 메커니즘은 **Kafka 자체 (broker/네트워크) 방어용이 아니라 downstream (Mongo/Redis/외부 API) 실패로부터 메시지를 보호하는 안전망**이다.

| 상황 | 처리 주체 |
|---|---|
| Kafka broker 죽음, 파티션 리밸런싱 | Kafka client 내부 재시도, offset 관리 |
| **Mongo 다운, Redis 끊김, FCM 500** | **DefaultErrorHandler + DLQ (이번 CH-1)** |

`processNotification` 내부에서 MongoDB 저장이 실패하면 그건 "이 메시지 처리 실패" 로 간주되어야 하고, 재시도/DLQ 안전망이 흡수해야 한다. 카오스 훈련의 CH-1 이 검증하려던 게 정확히 이 지점.

---

## 추가 발견 — DLQ 재처리 리스너도 같은 안티패턴

`toy-chat` — `UserNotificationConsumer.java:80-97` 의 `handleRetryNotification`:

```java
@KafkaListener(
    topics = "user.notifications.dlq",
    groupId = "notification-recovery",
    containerFactory = "notificationListenerFactory"
)
public void handleRetryNotification(...) {
  try {
    ...
    notificationService.processNotification(notificationDto);
    ack.acknowledge();
  } catch (Exception e) {
    log.error("[Kafka] DLQ 알림 재처리 실패: ...");
    ack.acknowledge();                    // ← 여기도 예외 삼킴 + ack
  }
}
```

- DLQ 에 메시지가 안 들어가는 지금 상황에서 **트리거 자체가 발생 안 함 → 데드코드**
- 위 catch 를 fix 해서 DLQ 로 메시지가 들어오기 시작해도, 이 리스너 역시 재처리 실패를 조용히 삼킴 → **fix 를 한 곳만 하면 DLQ 재처리 단계에서 또 유실**

---

## 아이러니 — 같은 프로젝트에 정반대 안티패턴 공존

커밋 `fca421f` (`ChatStorageConsumer`/`ChatNotificationConsumer` offset 미커밋 fix) 는 **ack 를 안 해서 rollout 시 재처리 폭발** 이 원인이었다. 이번 CH-1 발견은 **ack 를 무조건 해서 재시도/DLQ 무력화** 로 정반대 방향의 문제.

즉 이 프로젝트 컨슈머들의 **ack 정책이 통일돼 있지 않다**:

| 컨슈머 | ack 정책 | 결과 |
|---|---|---|
| `ChatStorageConsumer` (fca421f 이후) | finally 무조건 ack | rollout 시 재발사 방지 |
| `ChatNotificationConsumer` (fca421f 이후) | finally 무조건 ack | 위와 동일 |
| `UserNotificationConsumer.handleNotification` | catch 후 ack | **재시도/DLQ 무력화** |
| `UserNotificationConsumer.handleRetryNotification` | catch 후 ack | **DLQ 재처리 유실** |

컨슈머별로 원하는 실패 시맨틱이 다르니 통일 자체가 목적은 아니지만, **각 컨슈머가 어떤 시맨틱을 원하는지 의식적으로 결정** 되어 있어야 함. 지금은 두 컨슈머 (`UserNotification*`) 의 catch 가 **의도된 것인지 실수인지 코드만 봐선 알 수 없다.**

---

## 해결법

### 최소 diff — `UserNotificationConsumer.handleNotification`

```diff
  public void handleNotification(
      @Payload KafkaNotificationDto notificationDto,
      ...
      Acknowledgment ack) {

    try {
      notificationService.processNotification(notificationDto);
      ack.acknowledge();
-     log.info("[Kafka] 알림 처리 완료: ...");
+     log.info("[Kafka] 알림 처리 완료: userId={}, type={}",
+         notificationDto.getUserId(), notificationDto.getType());
    } catch (Exception e) {
      log.error("[Kafka] 알림 처리 실패: userId={}, type={}",
          notificationDto.getUserId(), notificationDto.getType(), e);
-     handleNotificationError(notificationDto, e, ack);
+     throw e;   // DefaultErrorHandler 에게 위임 → 재시도(1s×3) → DLQ
    }
  }

- private void handleNotificationError(...) { ... }   // 통째로 삭제
```

### `handleRetryNotification` 도 동일 원칙

DLQ 재처리 실패 시맨틱은 다시 결정 필요:
- (a) DLQ 재처리도 실패 시 예외 재발산 → `notification-recovery` group 이 무한 재시도 (재시도 back-off 필요)
- (b) 재처리 실패는 log + ack + 별도 알림 (현재 방식이지만 명시적으로 문서화)

**권장 (a)**: `handleRetryNotification` 도 `throw e` 로 위임. 단 DLQ 컨슈머의 error handler back-off 를 더 길게 (예: `FixedBackOff(60_000L, Long.MAX_VALUE)`) — 무한 재시도로 놓되 간격을 길게 잡아 운영 개입 시간 확보.

---

## DLQ 재처리 정책 재설계 — 왜 별도 factory 가 필요한가

### 원본 factory 를 그대로 쓰면 발생하는 문제

기존 `handleRetryNotification` 은 `containerFactory = "notificationListenerFactory"` 로 원본 토픽과 **같은 factory** 를 공유했다. 이 factory 의 error handler 는:

```java
new DefaultErrorHandler(
    deadLetterPublishingRecoverer(),   // DLQ 발행
    new FixedBackOff(1000L, 3)          // 1초 × 3회
);
```

이걸 DLQ 컨슈머가 그대로 쓰면:
1. DLQ 메시지 처리 → downstream 여전히 다운 → 실패
2. 1초 × 3회 재시도 → 여전히 실패
3. **`deadLetterPublishingRecoverer` 가 `user.notifications.dlq.dlq` 토픽으로 발행 시도**
4. `dlq.dlq` 토픽은 생성돼 있지도 않음 → NewTopic 자동생성 되거나 실패 → 무한 loop 또는 유실

### 재설계 원칙

**DLQ 컨슈머의 실패는 "downstream 아직 안 살아남" 이 대부분**. 그러니:
- 짧은 back-off (1초) 는 무의미 — downstream 이 1초 만에 살아날 리 없음
- DLQ 발행은 불필요 — 어차피 DLQ 에서 온 메시지, 더 격리할 곳 없음
- **간격 길게 (1분) + 무한 재시도** → downstream 복구까지 유지, 유실 없음

### 신규 `notificationDlqListenerFactory`

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, KafkaNotificationDto>
    notificationDlqListenerFactory() {
  ConcurrentKafkaListenerContainerFactory<String, KafkaNotificationDto> factory =
      new ConcurrentKafkaListenerContainerFactory<>();
  factory.setConsumerFactory(notificationConsumerFactory());
  factory.setConcurrency(1);
  factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

  // DLQ recoverer 없음 (DLQ 에서 온 메시지를 더 격리할 곳 없음)
  DefaultErrorHandler errorHandler = new DefaultErrorHandler(
      new FixedBackOff(60_000L, Long.MAX_VALUE)   // 1분 × 무한
  );
  errorHandler.addNotRetryableExceptions(SerializationException.class);
  observeRetries(errorHandler, "user-notification-dlq");
  factory.setCommonErrorHandler(errorHandler);
  return factory;
}
```

### 결과 흐름 (Mongo 다운 시나리오)

```
1. Mongo 다운
2. 원본 처리 실패 → 1s×3 재시도 → 모두 실패
3. DeadLetterPublishingRecoverer → user.notifications.dlq 로 발행 + offset 커밋
4. handleRetryNotification 이 DLQ poll 즉시 → 처리 시도
5. Mongo 여전히 다운 → 실패 → throw e
6. notificationDlqListenerFactory 의 error handler:
   1분 sleep → 재시도 → 여전히 다운 → 1분 sleep → ...
7. 운영자가 Mongo 복구
8. 다음 1분 재시도에서 성공 → ack → 오프셋 커밋
   → 유실 없음
```

### Trade-off

| 항목 | 즉시 재시도 (기존) | 1분 back-off (신규) |
|---|---|---|
| 유실 방지 | ✗ (catch+ack 로 유실) | ✓ 무한 유지 |
| 복구 지연 | 즉시 | 최대 1분 (마지막 재시도 후 1분 대기) |
| 리소스 부담 | 재시도 마다 poll | 1분에 1회 poll |
| 무한 loop 위험 | 있음 (기존 catch 없었다면) | 없음 (backoff 있으므로 CPU spike X) |
| 운영 개입 시간 | 없음 | 1분 여유 |

알림 도메인에서 **최대 1분 지연** 은 허용 가능. 결제 같은 실시간 도메인이면 back-off 를 30초로 줄이거나 alert 걸어 운영 즉시 개입.

### 완전 수동 재처리로 갈 경우

`handleRetryNotification` 자체를 `@KafkaListener` 로 두지 말고, **관리 API 엔드포인트** 로 바꾸는 대안:

```java
@PostMapping("/admin/dlq/notifications/replay")
public ReplayResult replayDlq(@RequestParam int maxMessages) {
    // KafkaConsumer 로 DLQ 소비 → processNotification 호출
}
```

운영자가 Mongo 복구 확인 후 명시적으로 트리거. **결제/포인트 같은 신중한 도메인** 에 적합. 알림 정도면 오버킬 — 현재 자동 재처리 유지.

---

## Fix 실행 결과 (2026-07-25)

> 커밋: `5eecb0a` (예외 삼킴 제거 + DLQ 팩토리), `cdca2a5` (재시도/DLQ 관측 로그),
> `a050e45` (Mongo 계측 contextProvider·중복 빈 정리) — 모두 toy-chat master, CI 배포됨.

### 변경된 파일

1. **`toy-chat/KafkaConsumerConfig.java`** — `notificationDlqListenerFactory` bean 신설 (line 275-297)
   - `FixedBackOff(60_000L, Long.MAX_VALUE)` — 1분 간격 무한 재시도
   - `observeRetries("user-notification-dlq")` — 재시도 시도마다 WARN, 소진 시 ERROR 로그

2. **`toy-chat/UserNotificationConsumer.java`** — 전면 재작성
   - `handleNotification`: `handleNotificationError` 호출 제거, `throw e` 로 위임
   - `handleRetryNotification`: `containerFactory` → `notificationDlqListenerFactory`, `throw e` 로 위임
   - `handleNotificationError` 메서드 삭제
   - javadoc 에 실패 정책 명시

---

## fix 후 검증 방법

### 1) 재현 (fix 후 재실행)

```bash
cd docs/chaos/scripts
./chaos.sh CH-1 on              # Mongo 다운 주입
sleep 90                        # ~1분 대기 (Loki 적재 시간)
./chaos.sh CH-1 symptom
./chaos.sh CH-1 off
```

### 2) 예상 로그 시퀀스 (fix 후)

```
[Kafka] 알림 처리 실패: userId=X, type=Y   ← 최초 실패
(Spring Kafka 재시도 1: 1초 후)
[Kafka] 알림 처리 실패: userId=X, type=Y   ← 재시도 1
(재시도 2)
[Kafka] 알림 처리 실패: userId=X, type=Y   ← 재시도 2
(재시도 3)
[Kafka] 알림 처리 실패: userId=X, type=Y   ← 재시도 3
Sending record to DLT: user.notifications-N@offset      ← DeadLetterPublishingRecoverer
[Kafka] DLQ 알림 재처리: userId=X, type=Y                  ← handleRetryNotification 트리거
```

### 3) DLQ 적재 확인

```bash
kubectl exec -it <kafka-pod> -- kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic user.notifications.dlq \
  --from-beginning --max-messages 10
```

Mongo 다운 기간 동안의 메시지 개수가 DLQ 에 있는지 확인 → 유실 없음 판정.

---

## Loki 스크립트 필터 변경

CH-1 스크립트의 로그 필터가 `|= "Retry"` 로 되어 있었으나 `UserNotificationConsumer` 는 "Retry" 라는 단어를 로그에 남기지 않음. 실제 로그 문구:
- `"[Kafka] 알림 처리 실패: userId={}, type={}, error={}"` (line 49)
- `"[Kafka] 알림 처리 오류: userId={}, type={}, error={}"` (line 61)

이번 실행부터 `|= "알림 처리 실패"` 로 변경. 더 넓게 잡으려면:

```
|~ "알림 처리 (실패|오류)"
```

fix 후엔 DLT publisher 로그 (`Sending record to DLT`) 도 시그널에 포함:

```
|~ "알림 처리 (실패|오류)|Sending record to DLT|DLQ 알림 재처리"
```

---

## 타이밍 노트 (다음 회차 재현 시)

- CH-1 안내에 "~1분 대기" 라고 되어 있으나 **Loki 적재 지연이 실측 ~1분**. 이번 회차엔 Enter 를 6초 만에 눌러 symptom 조회에서 최근 시그널이 안 잡혔음.
- 다음 회차부터 ④ (symptom 조회) 앞에서 **최소 90초, 안전하게 120초 대기**.
- Mongo 실제 다운 지속 시간: 이번 회차 16초. 재시도 총 시간 (1s × 3) 4초 안에 다 소모돼서 DLQ 로 갈 여유는 충분. **fix 후엔 유실 0건 이어야 정답**.

---

## 채점 앵커 갱신 제안

CH-1 `answer.md` 의 채점 앵커에 다음 조항 추가 검토 (§8.2 앵커 보강 절차 따름 — 이번 회차 아니라 **다음 회차부터** 적용):

- **오귀인 20 항목**에 "컨슈머 catch 로 예외 삼킨 상태를 '정상 격리' 로 오판" 을 감점 조건으로 추가
- 이번 회차는 fix 전 실행이므로 근거 시그널 도달 경로 자체가 물리적으로 불가능 → 이번 회차 채점은 유보 또는 별도 표시

---

## 후속 작업

- [x] `UserNotificationConsumer.handleNotification` catch 제거 (`throw e`)
- [x] `handleNotificationError` 메서드 삭제
- [x] `handleRetryNotification` 동일 fix (`throw e`) + `containerFactory` → `notificationDlqListenerFactory` 로 교체
- [x] `notificationDlqListenerFactory` bean 신설 (`FixedBackOff 1min × 무한`)
- [ ] fix 커밋 후 CH-1 재실행 → 위 예상 로그 시퀀스 확인
- [ ] DLQ 재처리 성공/실패 지표를 Grafana 대시보드에 추가 (`observeRetries` 가 남기는 `[KAFKA-RETRY]` 로그 기준)
- [x] `answer.md` 의 "발견된 계측 구멍 → 보강 커밋" 라인에 fix 커밋 SHA 기록 (2026-07-26)
- [ ] `ChatStorageConsumer` / `ChatNotificationConsumer` 도 실패 시맨틱 재점검 (`fca421f` 이후 finally ack 패턴이 재시도/DLQ 를 마찬가지로 무력화하는지 확인 — 별도 finding)
- [ ] 결제/포인트 등 신중한 도메인 컨슈머가 향후 생기면 자동 재처리 대신 관리 API 로 수동 재처리 검토
