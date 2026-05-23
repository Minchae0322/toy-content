# Hot Content Discovery Notification (보류 — main 미반영)

매일 새벽 4시에 가장 hot한 콘텐츠(Feed/Battle) 1건을 골라 푸시 동의자 전원에게 브로드캐스트하는 기능. **현재 main 머지 보류** — 아래 "위험 / 보류 사유" 참고.

브랜치: `feat/hot-content-discovery-notification`

## 동기

- 매일 1회 가벼운 재참여 유도 푸시
- 콘텐츠 종류 확장 가능해야 함 (현재 Feed, Battle / 향후 추가)
- 같은 콘텐츠는 중복 발송 금지

## 구조

```
HotContentDiscoveryNotificationScheduler  ← 4 AM cron, 단일 진입점
  ├─ HotContentSource (interface)         ← 확장 포인트
  │    ├─ FeedHotContentSource            ← QueryDSL: feed.hotScore desc
  │    └─ BattleHotContentSource          ← QueryDSL: 진행중 + battle.hotScore desc
  │
  ├─ HotContentNotificationSent (entity)  ← 공용 dedup
  │    unique(content_type, content_id)   ← 콘텐츠 단위 영구 1회
  │
  └─ NotificationService.broadcastHotContentDiscovery(candidate)
        KafkaNotificationDto.userId=null + channels=[PUSH]
```

### 선정 로직

1. 등록된 모든 `HotContentSource`에서 각 N건씩 후보 수집
2. `HotContentNotificationSent`에서 콘텐츠 종류별 IN 쿼리로 기 발송 ID 조회
3. 전체 후보를 `hotScore` 내림차순 정렬 → 기 발송이 아닌 첫 1건 선정
4. 브로드캐스트 + `(content_type, content_id)` sent 기록

### 확장 방법

새 콘텐츠 종류 추가:
1. `HotContentType` enum에 항목 1줄
2. `HotContentSource` 구현체 1개 (스프링 `@Component`)

스케줄러 본문 수정 불필요.

## 발송 채널

- `NotificationChannel.PUSH`만 사용 (인앱 알림함 미사용)
- 인앱에 N건씩 쌓이는 폭증을 피하려는 의도. 푸시는 컨슈머 측에서 FCM 동의자 필터링 후 전송.

## ⚠️ 위험 / 보류 사유

**브로드캐스트 마커로 `userId=null`을 사용했는데, 이게 main 머지를 막는 가장 큰 이슈.**

기존 `NotificationService`의 다른 메서드들은 `userId`가 반드시 채워진다는 암묵적 계약을 가지고 있다. 그런데 동일 DTO(`KafkaNotificationDto`)에 `userId=null`이 "브로드캐스트"라는 의미를 갖게 되면:

- 어떤 신규/기존 코드 경로에서 `userId`를 실수로 null로 넣는 순간 → **전체 사용자 푸시 폭발**
- 컴파일러/리뷰어가 잡기 어려움. 단순 NPE 가드도 안 통함 (null이 "유효한 의미"가 되어버렸으므로)

블래스트 라디우스가 너무 큼.

## 머지 전 정리 옵션

다음 중 하나로 명시화하면 안전:

### A. 별도 카프카 토픽 분리 (권장)

- `spring.kafka.topic.notification` (per-user) vs `spring.kafka.topic.notification.broadcast` (broadcast)
- 토픽 자체가 의미를 분리 → 실수로 broadcast 토픽에 메시지가 안 들어감
- 컨슈머 측도 토픽별로 핸들러 분리 (per-user는 그대로, broadcast는 FCM fanout)

### B. DTO에 `broadcast: boolean` 명시 필드 추가

- `KafkaNotificationDto.broadcast = true`일 때만 브로드캐스트 처리
- userId null 자체는 여전히 invalid로 가드 가능
- 단일 토픽 유지하되 의미만 명시

### C. 별도 DTO 클래스 (`BroadcastNotificationDto`)

- 타입 자체로 구분, 가장 강한 컴파일 타임 안전
- 다만 컨슈머 측 역직렬화 분기가 더 복잡

### D. 푸시 동의자 ID 리스트 chunk 발송

- 외부 UserService에 `GET /external/users/push-consenting` 엔드포인트 추가
- 받아온 ID를 chunk(예: 1000건)로 끊어 per-user 메시지 N건 발행
- 카프카/컨슈머 변경 없음, 다만 외부 API 변경 + 발행 트래픽 증가
- 인앱 알림함도 자연스럽게 채움(원래 PUSH-only로 설계했으나 옵션 가능)

## 추후 액션

- [ ] 컨슈머 레포(외부)와 채널 분리 방식 협의 (A 또는 B 권장)
- [ ] 위 옵션 중 선택해 `NotificationService.broadcastHotContentDiscovery` 발행 경로 변경
- [ ] 컨슈머 측 fanout/FCM 핸들러 구현 확인 후 main 머지

## 현재 브랜치에 포함된 변경

**신규 파일**
- `src/main/java/com/example/toycontent/app/notification/hotcontent/HotContentType.java`
- `src/main/java/com/example/toycontent/app/notification/hotcontent/HotContentCandidate.java`
- `src/main/java/com/example/toycontent/app/notification/hotcontent/HotContentSource.java`
- `src/main/java/com/example/toycontent/app/notification/hotcontent/FeedHotContentSource.java`
- `src/main/java/com/example/toycontent/app/notification/hotcontent/BattleHotContentSource.java`
- `src/main/java/com/example/toycontent/app/notification/hotcontent/HotContentNotificationSent.java`
- `src/main/java/com/example/toycontent/app/notification/hotcontent/HotContentNotificationSentRepository.java`
- `src/main/java/com/example/toycontent/app/scheduler/HotContentDiscoveryNotificationScheduler.java`
- `src/test/java/com/example/toycontent/app/scheduler/HotContentDiscoveryNotificationSchedulerTest.java` (5건 통과)

**수정 파일**
- `src/main/java/com/example/toycontent/app/common/enumuration/NotificationType.java` — `HOT_CONTENT_DISCOVERY` 추가
- `src/main/java/com/example/toycontent/app/notification/NotificationService.java` — `broadcastHotContentDiscovery(...)` 추가 (위 위험 항목 해당)
