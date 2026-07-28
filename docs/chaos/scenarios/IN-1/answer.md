# IN-1 — Redis 다운 (다중 서비스 복합, 최고 난도)

> Then. Given/When은 [RUNBOOK.md](../../RUNBOOK.md) §6 IN-1. 여기엔 실행 결과를 남긴다.

- **주입**: `docker stop $REDIS_CT` (infra 노드)
- **hop**: 다중
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

Redis 다운. 캐시·분산락·프레즌스·인증코드가 한꺼번에 무너지되 서비스별 증상이 다름.

## 근거 시그널 도달 경로

핵심: 서비스별로 다르게 아픈 증상을 **단일 근원(Redis)으로 수렴**시키는 능력이 채점 포인트.
- content: user 캐시 미스 → auth 직행 호출 + ShedLock 실패 → 스케줄러 skip → 핫스코어 갱신 정체
- chat: 프레즌스 조회 실패 → WS/FCM 이상
- auth: 이메일 인증·위치 검색 실패

**실측 확인된 도달 경로 (2026-07-28, 주입 없이 코드·설정에서 확인)**

| # | 신호 | 근거 | 채널 |
|---|---|---|---|
| ① | **Lettuce 커맨드 span**이 에러/타임아웃 — content·chat **양쪽** | `app/config/TracingConfig.java:29` · toy-chat `app/config/ObservabilityConfig.java:46` (`MicrometerTracing(registry, "redis")` 수동 등록) | trace |
| ② | **Redis 실패 로그가 서로 다른 `service_name`에서 동시에** | 아래 동치 집합 — 어느 조합이든 인정 | Loki |
| ③ | Redis 미스 직후 **auth HTTP client span**으로 직행 | `UserCacheStore.getCachedUserInfos` → 미스 시 외부 호출 | trace |
| ④ | **WS 세션은 정상인데 프레즌스만 깨짐** | `websocket.active_users`는 `SimpUserRegistry`(파드 로컬 STOMP) 기반이라 **Redis와 무관하게 유지된다** — toy-chat `app/config/WebSocketMetricsConfig.java:30` | Mimir + Loki |

②가 이 문항의 **핵심 지문**이다. rca-agent의 `errorWarnQuery`는 **라인 필터**라
`{service_name=~"content-service|auth-service|chat-service"}` 전체를 한 번에 훑는다 —
**동일 계열 실패가 `service_name`만 다르게 동시에 뜨는 것** 자체가 "서비스별 개별 장애가
아니라 공통 의존성 하나"라는 증거다. traceId 없이도 도달한다.

### ②의 동치 집합 — **특정 문구를 요구하지 않는다**

Redis 실패는 서비스마다 다른 문구로 새어 나온다. **어느 것으로 도달하든 같은 값**이다.

| 서비스 | 문구 | 위치 |
|---|---|---|
| content | `Redis 캐시 조회 실패: cacheKey=...` / `Redis 캐시 저장 실패` | `UserCacheStore.java:51` `:79` |
| chat (캐시) | `Redis 캐시 조회 실패: cacheKey=...` — **content와 동일 문구** | toy-chat `UserInfoCacheService.java:50` |
| chat (프레즌스 **쓰기**) | `Error setting device online` / `Error setting device offline` / `Error leaving chat room` / `Error removing device from all chat rooms` | toy-chat `ClusterPresenceStore.java:71` `:93` `:125` `:266` |
| chat (프레즌스 **읽기**) | `❌ [STOMP CHATROOM SUBSCRIBE] Error` / `[STOMP PERSONAL SUBSCRIBE] Error` / `Error handling STOMP CONNECT` / `[STOMP DISCONNECT] Error` | toy-chat `Stomp*Handler.java:65` `:66` `:79` `:38` |

> **채점 시 특정 경로를 지정해 요구하지 말 것.** "프레즌스 로그를 봤는가"가 아니라
> **"Redis 실패가 두 서비스 이상에서 동시에 관측됨을 보였는가"**로 판정한다.
> §8.2 충분 근거 원칙 — 요건은 신호 쇼핑 목록이 아니라 **동치 집합**이다.

### 프레즌스가 특히 유용한 이유 — 쓰기/읽기 처리가 다르다

`ClusterPresenceStore`는 **쓰기는 삼키고 읽기는 던진다.**

| 경로 | 메서드 | Redis 다운 시 |
|---|---|---|
| **쓰기** | `setDeviceOnline`(`:55`) · `setDeviceOffline`(`:81`) · `leaveChatRoom`(`:115`) · `removeDeviceFromAllChatRooms`(`:257`) | try/catch로 **삼킴** → `log.error`만 남고 **연결은 성공한 것처럼 보인다** |
| **읽기** | `isDeviceOnline`(`:132`) · `enterChatRoom`(`:100`) · `getChatRoomMembers`(`:165`) · `getOnlineUsers`(`:206`) · `getUserOnlineDevices`(`:242`) | **try/catch 없음 → 예외 전파** → STOMP 핸들러가 ERROR로 기록 |

그래서 사용자 증상이 **"접속은 되는데 남들 눈엔 오프라인"** 또는 **"채팅방에 못 들어가진다"**로
나타난다. 연결 자체가 끊기는 게 아니라 **연결과 상태가 어긋난다.**

**④가 chat 앱 자체 장애를 죽인다.** `websocket.active_users`는 파드 로컬 세션 수라
Redis와 무관하게 **평시 수준을 유지한다.** *"세션은 멀쩡한데 프레즌스만 깨졌다"*는
chat 프로세스·네트워크 문제로는 설명되지 않고 **공유 저장소 하나**를 가리킨다.
`websocket_active_users`는 이미 rca-agent `metric-queries`에 있다(CH-2용으로 추가).

> **스케줄러 skip은 요건에서 뺐다 (v2).** cron이 `0 0 * * * *`·`0 30 * * * *`
> (`application-prod.yml:133,140,151`)인데 rca-agent 수집 창은 **트레이스 ±120초(≈4분)**다.
> **매시 정각 ±2분에 주입하지 않는 한 창에 들어오지 않는다** — 관측 가능성이 주입 시각이라는
> 우연에 걸린 요건은 채점 기준이 될 수 없다(유형 B: 입력 범위 무시).

## 채점 앵커 — **v2 (2026-07-28 개정 · 미실행 문항이라 §8.2상 개정 가능)**

> **v1 정정 사유**: v1의 근거 경로 요건이 *"content(캐시 미스 직행 급증)·chat(프레즌스)·
> 스케줄러 skip 중 2개 서비스 이상"*이었는데, **세 신호가 각각 어떤 span/로그인지 정의되지
> 않았고** 그중 스케줄러 skip은 수집 창 밖이며 "급증"은 traceId 1개로 볼 수 없다.
> 위 실측 표의 ①②③으로 문언을 교체한다.

| 항목 | 만점 | 부분점(상) | 부분점(하) | 0점 |
|---|---|---|---|---|
| 근본 원인 40 | 서비스별 상이한 증상을 **Redis(공통 캐시) 단일 근원으로 수렴** + **Redis 자체가 응답 불가**임을 명시 | 30: Redis 의존 경로 실패까지 짚었으나 여러 서비스를 엮지 못함 | 20: 증상 나열은 정확하나 근원 미수렴(개별 장애 취급) · **10: 캐시 미스를 정상 동작으로 오판** | 다른 근원 지목(DB·네트워크·auth 등) |
| 근거 경로 30 | **①②③④ 중 둘 이상 + 교차 서비스성**(같은 실패가 `service_name`이 다른 곳에서 동시 발생)을 명시 | 20: ①②③④ 중 **둘** (교차 서비스성 미명시) | 10: **하나만** / 또는 "캐시가 안 된다"만 서술하고 신호 인용 없음 | 관측 근거 없이 추측 |
| 오귀인 20 | 3개 서비스 개별 장애 3건으로 **쪼개지 않음** + **auth를 원인으로 지목하지 않음**(직행 호출 증가는 결과) | 10: 쪼개지는 않았으나 근거 없음 | — | 서비스별 장애로 쪼갬 · auth/DB를 근원으로 지목 |
| 조치 10 | Redis 복구 **+ 복구 후 캐시 재적재/정상화 확인** | 5: 복구만 | — | 무관 조치 |

**설계 의도 — "여러 개가 아프다"에서 "하나가 죽었다"로 가는 추론을 잰다.**

이 문항의 함정은 **증상이 서비스마다 다르다는 것**이다. content는 auth 호출이 늘고,
chat은 프레즌스가 깨진다 — 표면만 보면 별개 장애 세 건이다. 그래서 오귀인 항목에
**"쪼개지 않음"**을 명시적으로 걸었고, 근거 경로 만점 요건에 **"같은 실패 문구가 서로 다른
`service_name`에서"**를 넣었다. 단일 근원의 증거는 신호의 *개수*가 아니라 **신호의 동형성**이다.

**auth 오귀인이 이 문항의 두 번째 함정이다.** Redis가 죽으면 캐시 미스로 auth 직행 호출이
늘어 auth가 느려 보인다 — **결과를 원인으로 착각하기 좋은 구조**다. AU-2(auth 다운)와
증상이 겹치되 근원이 반대라서, 둘을 가르는 것이 ①(Redis 커맨드 span 자체의 실패)이다.

### 박제 전 검증 체크리스트

- [ ] Redis 다운 시 **Lettuce span이 실제로 남는가** — 에러 태그인지, 타임아웃으로 늘어진
      span인지, 아니면 **아예 사라지는지**(연결 실패면 커맨드가 시작조차 안 될 수 있다).
      **사라진다면 ①은 "부재 신호"로 문언을 바꿔야 한다**
- [ ] `Redis 캐시 조회 실패` 로그에 **traceId가 실제로 찍히는가** (요청 스레드 전제 확인)
- [ ] **프레즌스 읽기 예외가 실제로 STOMP 핸들러까지 올라오는가** — `enterChatRoom`·
      `isDeviceOnline`에 try/catch가 없어 전파될 것으로 읽었다(`ClusterPresenceStore:100,132`).
      중간에 삼키는 곳이 있으면 ②의 프레즌스 항목을 빼거나 문언을 바꾼다
- [ ] **`websocket.active_users`가 Redis 다운 중에도 유지되는가** — ④의 전제.
      `SimpUserRegistry`는 로컬 세션이라 유지될 것으로 읽었으나, CONNECT 핸들러가 예외로
      세션을 못 만들면 **함께 떨어진다**. 떨어지면 ④는 성립하지 않는다
- [ ] `up`/`redis_up` 계열 메트릭이 있는가 — 있으면 근거 경로에 추가 검토(현재 `metric-queries`에 **없다**)

> 위 항목이 실측과 다르면 **앵커를 실측에 맞춰 고친 뒤 주입한다.** 코드 독해를 실측으로
> 착각해 채점 불가가 난 것이 [유형 C](../../../../../yogurtte-rca-agent/docs/scoring/README.md)(AU-4)다.

## RCA 채점 (블라인드 — §8 루브릭, 회차는 §8.1 반복 프로토콜)

| 항목 | 배점 | 1회 | 2회 | 3회 |
|---|---|---|---|---|
| 근본 원인 정확도 | 40 | | | |
| 근거 시그널 경로 | 30 | | | |
| 오귀인 없음 | 20 | | | |
| 조치 타당성 | 10 | | | |
| **합계** | **100** | | | |

- 평균 ± 최대편차: (±10 초과 시 문항 불안정 — §8.1)
- 자기 일치도(동일 출력 2회 채점 차, ±5 초과 시 앵커 보강 — §8.2):

## evidence

- baseline: `evidence/baseline/<ts>/`
- symptom: `evidence/symptom/<ts>/`
- 발견된 계측 구멍 → 보강 커밋:
