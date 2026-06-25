# 트랜잭션 커밋과 알림 발행의 순서 — "유령 알림"을 구조적으로 차단하기

> 배틀 댓글 알림에서 실제로 존재하던 잠재 결함(롤백된 댓글에 대한 알림 발송)을 발견하고,
> `@Async` 직접 발행을 `@TransactionalEventListener(AFTER_COMMIT)` 이벤트 발행으로 바꿔
> 도메인 요구사항에 맞는 최소 비용으로 차단한 기록.

---

## 한 줄 요약

알림은 "콘텐츠가 실제로 저장된 뒤"에만 의미가 있다. 그런데 우리 알림 발행은 `@Async`로
트랜잭션 경계를 즉시 벗어나고 있었고, 배틀 댓글의 경우 **저장보다 먼저** 알림을 호출하고 있었다.
트랜잭션이 롤백되면 존재하지 않는 댓글에 대한 알림(유령 알림)이 사용자에게 도달할 수 있는 구조였다.
발행 시점을 **커밋 이후**로 옮겨 이 결함을 구조적으로 제거했다.

---

## 1. 배경 — 우리 알림은 유실은 허용하지만, 유령은 허용하지 않는다

알림(`content-service` 성격)은 콘텐츠가 먼저 저장된 뒤 발생하는 후속 작업이다.
이 도메인의 요구사항을 정리하면 이렇다.

| 요구사항 | 수준 |
|---|---|
| 알림 1건 유실 | **허용** (콘텐츠 정합성을 깨지 않음) |
| 존재하지 않는 콘텐츠에 대한 알림(유령) | **불허** (사용자가 클릭하면 404, 신뢰 저하) |
| 발행 100% 보장(Outbox) | 과설계 (유실 허용 도메인) |

즉 **유실(at-most-once)과 유령(phantom event)은 별개의 문제**다. 유실은 받아들이되,
유령은 막아야 한다. 이 한 줄이 이후 설계 판단의 기준이 된다.

---

## 2. 발견 — `@Async`는 트랜잭션을 기다려주지 않는다

`NotificationService`의 모든 발행 메서드는 `@Async`였다.

```java
// (개선 전) NotificationService
@Async("notificationExecutor")
public void notifyBattleItemComment(...) {
    sendSafely(type, dto); // 내부에서 kafkaProducer.send(dto)
}
```

`@Async`는 호출 즉시 별도 스레드 풀(`notificationExecutor`)로 작업을 넘긴다.
문제는 이게 **호출자의 트랜잭션과 완전히 분리된 스레드**라는 점이다. 트랜잭션이 아직
커밋되지 않았어도, 심지어 이후에 롤백되더라도, 알림 발행은 그대로 진행된다.

그리고 배틀 댓글 생성 로직에서는 호출 순서까지 거꾸로였다.

```java
// (개선 전) BattleItemCommentService.createComment
battleItem.getBattle().incrementTotalCommentCount();

notificationService.notifyBattleItemComment(...); // ← 알림 먼저 (별도 스레드로 발행 시작)

commentRepository.save(comment);                  // ← 저장은 그 다음
```

`save()`가 제약 위반 등으로 실패하면 댓글은 생기지 않지만, 알림은 이미 다른 스레드에서
날아간 뒤다. **배틀 댓글 유령 알림**이 코드상 실재하는 시나리오였다.

> 정리: `@Async` + 트랜잭션 안 호출 = 커밋과 무관한 즉시 발행.
> 여기에 "저장보다 먼저 호출"까지 겹쳐 유령 알림 가능성이 두 겹으로 존재했다.

---

## 3. 선택지 비교 — 유령만 막으면 되는 도메인

| 방식 | 유령 알림 | 유실 | 비용 | 이 도메인 적합성 |
|---|---|---|---|---|
| 트랜잭션 안에서 직접 `send()` / `@Async` 직접 발행 | 발생 가능 ❌ | 발생 가능 | 낮음 | 부적합 |
| **`@TransactionalEventListener(AFTER_COMMIT)`** | **차단 ✅** | 발생 가능(허용됨) | 낮음 | **적합** |
| Transactional Outbox | 차단 ✅ | 차단 ✅ | 높음 | 과설계 |

유실까지 막아야 한다면 Outbox(발행 100% 보장)가 정답이다. 하지만 이 도메인은 유실을
허용한다. **유령만 차단하면 충분**하므로, Outbox 테이블·중계·운영 부담 없이 가벼운
`AFTER_COMMIT` 방식을 택했다.

---

## 4. 개선 — 발행을 "커밋 이후"로 미루는 이벤트 한 겹

핵심 아이디어: 서비스는 트랜잭션 안에서 **이벤트만 등록**하고, 실제 Kafka 발행은
**커밋이 성공한 뒤**에만 일어나게 한다.

### 4-1. 인-프로세스 이벤트

```java
public record NotificationEvent(KafkaNotificationDto payload) {
}
```

### 4-2. 발행은 커밋 이후에만 — 리스너

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final KafkaNotificationProducer notificationProducer;

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onNotification(NotificationEvent event) {
    KafkaNotificationDto dto = event.payload();
    try {
      notificationProducer.send(dto);
    } catch (Exception e) {
      log.error("[Notification] 알림 발행 실패: userId={}, type={}, error={}",
          dto.getUserId(), dto.getType(), e.getMessage(), e);
    }
  }
}
```

세 가지 설정의 의도:

- **`AFTER_COMMIT`**: 커밋이 성공한 경우에만 리스너가 실행된다. 롤백되면 이벤트는
  폐기되어 발행 자체가 일어나지 않는다 → 유령 알림 구조적 차단.
- **`fallbackExecution = true`**: 트랜잭션 경계 밖에서 발행된 이벤트도 그대로 처리한다.
  (트랜잭션 없이 호출되는 경로에 대한 안전장치 — 이 경우 즉시 실행된다.)
- **`@Async("notificationExecutor")`**: 커밋 스레드를 막지 않고 기존 알림 전용 풀에서
  비동기 발행. 기존의 "요청 스레드를 막지 않는다"는 성질을 그대로 유지했다.

### 4-3. 서비스는 이벤트만 등록

`NotificationService`의 공개 API(`notifyXxx`)는 **그대로 두고**, 내부만 바꿨다.
`@Async`를 떼어 호출자의 트랜잭션 스레드에서 동기로 실행되게 하고(그래야
`AFTER_COMMIT` 동기화가 등록된다), 실제 발행 대신 이벤트를 publish 한다.

```java
// (개선 후) NotificationService
public void notifyBattleItemComment(...) {
    if (battleItemCreatorId.equals(actorId)) return;
    sendSafely(type, KafkaNotificationDto.builder()...build());
}

private void sendSafely(NotificationType type, KafkaNotificationDto dto) {
    eventPublisher.publishEvent(new NotificationEvent(dto)); // 직접 send 하지 않는다
}
```

공개 API를 유지했기 때문에 **피드·배틀·스케줄러 등 모든 호출부(약 10곳)가 코드 변경
없이** 커밋-이후 발행의 혜택을 받는다. 이게 이 리팩터링에서 가장 마음에 드는 부분이다 —
새로 추가한 파일은 이벤트 레코드 1개와 리스너 1개뿐이다.

### 4-4. 호출 순서도 바로잡기

배틀 댓글 쪽은 순서까지 정리했다. 저장을 먼저 하고 그 다음 알림 이벤트를 등록한다.
이제 발행은 어차피 커밋 이후로 미뤄지지만, 코드 흐름도 "저장 → 알림"으로 읽히게 했다.

```java
// (개선 후) BattleItemCommentService.createComment
commentRepository.save(comment);
// 알림은 이벤트로 등록만. 실제 발행은 커밋 이후 → 롤백되면 함께 폐기(유령 방지)
notificationService.notifyBattleItemComment(...);
```

---

## 5. 흐름 비교

```
[개선 전]
 createComment (TX)
   ├─ notify(...)  ──@Async──▶ [다른 스레드] kafka.send()   ← 커밋 전에 이미 발행
   └─ save(comment)  ── 실패/롤백 ──▶ 댓글 없음 + 알림은 이미 발송 = 유령

[개선 후]
 createComment (TX)
   ├─ save(comment)
   └─ notify(...) ─▶ publishEvent(NotificationEvent)  ← 등록만
        └─ 커밋 성공 ─AFTER_COMMIT─▶ @Async ─▶ kafka.send()
           커밋 롤백 ─▶ 이벤트 폐기 ─▶ 발행 없음
```

---

## 6. 알아둘 점 / 한계

- **AFTER_COMMIT 단계의 예외는 이미 커밋된 트랜잭션을 롤백시키지 못한다.** 따라서 이
  구간의 발행 실패는 별도 처리(로깅·모니터링)가 필요하다. 본 도메인은 유실 허용이므로
  리스너에서 예외를 잡아 로깅으로 보완했다. (유실을 못 막는 도메인이라면 여기서
  Outbox로 올라가야 한다.)
- **신뢰성 보강은 발행 경로에 이미 있다.** producer는 `acks=all` +
  `enable.idempotence=true` + `retries=3`으로 구성되어 있다. 다만 현재 토픽은 단일
  브로커(`replicas=1`)라 `acks=all`의 내구성 효과는 멀티 브로커 + `min.insync.replicas≥2`
  전환 시점에 온전해진다. (지금은 "전환 시 의미를 갖는 설정"으로 깔아둔 상태.)
- **순서/파티셔닝**: 알림 토픽 key는 `userId`다. 같은 사용자의 알림이 같은 파티션으로
  가 순서가 유지된다.

---

## 7. 검증

- `NotificationServiceTest` — `notifyXxx` 호출 시 producer를 직접 부르지 않고
  `NotificationEvent`를 publish 하는지, 본인 행위는 발행하지 않는지 검증.
- `NotificationEventListenerTest` — 리스너가 이벤트 수신 시 producer로 발행하는지,
  producer 예외를 삼켜 정상 반환하는지(유실 허용) 검증.
- `BattleItemCommentServiceTest` — 댓글 저장/알림 호출 순서 변경 후에도 기존 동작
  (댓글 수 증가, 알림 호출, 예외 시 무발행) 유지 확인.

---

## 8. 한 문장 회고

"Outbox를 아느냐"가 아니라 **"이 도메인엔 무엇이 과하고 무엇이 부족한가"** 를 판단하는
문제였다. 유실은 허용하되 유령은 막아야 하는 알림에는, Outbox가 아니라
`@TransactionalEventListener(AFTER_COMMIT)` 한 겹이 정확히 들어맞는 비용이었다.
