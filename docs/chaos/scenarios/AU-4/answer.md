# AU-4 — auth 완전 다운 + user 캐시 만료 (fallback 경로 실검증)

> Then. Given/When(전제·주입·판정 기준)은 [RUNBOOK.md](../../RUNBOOK.md) §6 AU-4. 여기엔 실행 결과를 남긴다.
> AU-2에서 파생 신설(2026-07-26): AU-2는 캐시 히트라 content 무영향(실명 유지)이라, "auth 죽고 캐시도 없을 때"의 fallback 경로가 검증되지 않았다. 그 구멍을 메우는 문항.

- **주입**: `kubectl scale deploy/$AUTH_DEPLOY --replicas=0` (AU-2와 동일) + **10분+ 유지**(캐시 TTL 경과 대기)
- **hop**: 2~3 (content → user 캐시 미스 → auth 직행 실패 → fallback)
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

auth 전면 다운 상태에서 user 캐시(TTL 10분)까지 만료 → content가 auth 직행 → **Connection refused**(auth Pod 0 → Service Endpoints 부재 → TCP 즉시 거절, ~23.5ms) → `createFallbackUserInfo`(익명 "사용자N")로 저하. **원인은 auth 다운이고, content 500이면 fallback 붕괴(별개 실버그)**.

> **정정 (2026-07-28)**: 구 "3s timeout" → "Connection refused". `Duration.ofSeconds(3)`은 auth Pod가 *살아있으나 무응답*일 때만 발동하는 상한이고, `replicas=0` 주입은 Endpoints가 비어 커넥션이 **즉시 거절**된다(타임아웃 아님). 구 회차 1(07-27 07:00Z)에서 실측·확정된 사실이며(유형 C 앵커 오류 — [AE-06](../../../../../yogurtte-rca-agent/docs/findings/ae-06-rca-v0-au4-blind-eval.md)), 채점 대장 앵커 작성 체크리스트("코드 독해를 실측으로 착각 금지")의 근거가 됐다. 이 정정은 재량이 아니라 인프라 사실이며 에이전트 출력과 무관하다.

## AU-2와의 구별 (이 문항의 핵심)

| | AU-2 | AU-4 |
|---|---|---|
| 주입 | auth scale 0 | auth scale 0 + **10분+ 유지** |
| 캐시 | 히트 (10분 내) | **만료** (10분 경과) |
| content 피드 작성자 | 실명 유지 (무영향) | 익명 "사용자N" (fallback) |
| 검증 대상 | 직접 경로만 죽음 (decoupling) | **fallback 경로 자체** (익명 저하 vs 붕괴) |
| content client span | (호출 안 함 — 캐시 히트) | connection refused/timeout 후 fallback |

## 근거 시그널 도달 경로

채록하며 채운다.
핵심: T2(`?size=10`) 응답의 작성자 nickname이 "사용자N" 익명인가 — `ExternalUserApiClient`가 auth에 **Connection refused**(Endpoints 부재) 후 `createFallbackUserInfo` 경로. (`Duration.ofSeconds(3)` timeout은 Pod가 살아있으나 무응답일 때만 — replicas=0에선 즉시 거절.) 캐시 만료 전(10분 내)에는 실명이라 AU-2와 구별 안 됨.

## 전제 (§10)

- **user 캐시 TTL = 10분** (`UserCacheStore.DEFAULT_TTL = Duration.ofMinutes(10)`). 이보다 짧게 주입하면 캐시 히트라 AU-2와 동일 — 반드시 10분+ 유지.
- content fallback 코드 존재 확인됨: `ExternalUserInfoService.getUserInfo`가 "절대 null 반환 안 함", API 실패 시 `createFallbackUserInfo`. **이 fallback이 실제로 버티는지가 검증 대상.**
- `/feeds/scroll`은 `?size=` 필수 (미지정 시 NPE 500 — content `1e7df3f`로 수정, 그 전 chaos.sh t2는 size 없이 호출해 오염됐음).

## 채점 앵커 — **v3 (2026-07-27, 회차 1 실측 반영 · 회차 2부터 적용)**

> **회차 1은 v1으로 채점했다.** v2 개정과 채록의 선후를 확정할 수 없어 §8.2에 따라 v1을
> 적용했고, 결과는 **채점 불가(앵커 부적합)** — 채점된 3항목은 70/70 만점이었다.
>
> **v2 → v3 정정 사유 (회차 1 실측이 v2를 반증했다)**:
> - v2 근거경로 만점이 요구한 **"3s timeout"이 발생하지 않는다.** 실측은
>   **23.5ms connection refused** — `.timeout(3s)`는 *대기 상한*이라 TCP RST에는 발동하지 않는다.
>   코드에 상수가 있다고 그 경로가 실행되는 게 아니다.
> - v2가 요구한 **"fallback 로그"는 존재하지만 `traceId=NONE`**이다
>   (`ExternalUserApiClient.java:130` `.doOnError`가 Reactor 이벤트 루프에서 실행돼 MDC 미전파).
>   로그가 있어도 traceId 기반 조사로는 도달할 수 없으므로 만점 요건에서 뺐다.
> - 런북·앵커가 찾으라 한 **`GET user-service` client span은 존재하지 않는다.**
>   실제 span 이름은 `http get`이고 서비스 식별은 `client.name=auth-service` 속성에 있다.
>
> **교훈**: 앵커의 만점 요건은 **baseline 채록에서 실물로 확인된 신호**로만 구성한다.
> 코드 독해는 가설이지 근거가 아니다. 상세는 rca-agent `docs/scoring/README.md` 결함 ⑩.

**갈래 판정 (채점 전에 먼저 확정한다)**: 캐시 만료 후 T2 응답이
**작성자 익명 `사용자N` + HTTP 200이면 갈래 A(fallback 정상 저하)**,
**HTTP 5xx면 갈래 B(fallback 붕괴 — 실버그)**. 두 갈래는 정답지가 다르므로 갈래를 먼저
정하고 해당 앵커로만 채점한다.

**영향 판정 용어**: AU-4는 알림 도착이 아니라 **데이터 품질 저하**가 증상이므로 "10분 내
미도착" 대신 **"저하(degraded) vs 붕괴(broken)"**로 판정한다. 판정 근거는 T2의 HTTP 코드와
작성자 nickname 실물이다.

### 갈래 A — fallback 정상 (익명 저하, 200 유지)

| 항목 | 만점 | 부분점(상) | 부분점(하) | 0점 |
|---|---|---|---|---|
| 근본 원인 40 | auth 전면 다운 + **user 캐시 만료**로 content가 auth 직행 → fallback 발동(익명 저하). 두 조건의 결합까지 연결 | 30: auth 다운은 확정, 캐시 만료라는 **발현 조건**을 미언급 | 20: "auth 이상"까지만 / 저하를 장애(붕괴)로 **오판** | content·Redis 자체 장애로 지목 |
| 근거 경로 30 | auth 호출 client span의 error를 근거로 쓰되 **실패 방식을 구별**(즉시 거절 vs 3s timeout) + baseline 대비 **auth 서버 span의 출현/부재 대조** + 작성자 익명 전환 | 20: 3종 중 2종 / 또는 **동등한 대체 경로로 도달**(예: auth 메트릭 시계열 소멸을 대조군과 함께 제시) | 10: 작성자 익명 또는 500만 인용 | 근거 없음 |
| 오귀인 20 | content·Redis를 원인으로 지목 안 함 + **익명 저하를 "정상 동작"으로 인식**(fallback이 의도대로 버틴 것) | 10: 둘 중 하나만 | — | 피드 이상을 content 탓으로 (AU-2 회차0 오염 재현) |
| 조치 10 | auth 복구 + **캐시 TTL·워밍 검토**(캐시가 가려주던 의존을 드러낸 것이 이 문항의 교훈) | 5: auth 복구만 | — | 무관 조치 |

> **fallback 로그는 현행 v0로 관측 불가하다** — Loki 셀렉터 결함이 5회 연속이다. 그래도
> 만점 요건에 **남긴다**: 3종 중 2종(timeout span·익명 전환)은 trace와 응답으로 도달
> 가능하므로 부분점(상) 20은 충분히 나오고, 셀렉터를 고치면 30이 되어야 한다.
> **의도된 개선 측정 지점**이다(CH-1 갈래 B와 같은 취급).

### 갈래 B — fallback 붕괴 (5xx, 실버그)

| 항목 | 만점 | 부분점(상) | 부분점(하) | 0점 |
|---|---|---|---|---|
| 근본 원인 40 | auth 다운 + 캐시 만료가 **방아쇠**이고, 5xx의 직접 원인은 **content의 fallback 경로 결함**임을 분리 | 30: auth 다운은 확정, fallback 결함과의 분리가 모호 | 20: "auth 다운 때문에 피드가 죽었다"로 뭉뚱그림 | Redis·DB 등 무관 컴포넌트 지목 |
| 근거 경로 30 | client span 실패 → **fallback 진입 후의 예외**(스택/에러 span)를 근거로 "저하가 아니라 붕괴"임을 입증 | 20: 5xx와 auth 다운의 시간적 일치까지 | 10: 5xx만 인용 | 근거 없음 |
| 오귀인 20 | auth 다운을 원인으로 **지목하되**, 5xx의 책임을 content fallback에 정확히 귀속 | 10: 둘의 구분 없이 auth만 지목 | — | content를 아예 무관하다고 판정 |
| 조치 10 | auth 복구 + **fallback 경로 수정**(별개 결함으로 티켓 분리) | 5: auth 복구만 | — | 무관 조치 |

## 채록 전 준비 (AU-2 회차 1의 교훈)

- [ ] **소급이 아니라 실시간 채록.** `measure_AU_4`는 `loki_count`(fallback)·`prom`(client p99)을
      호출하므로 AU-2보다 낫지만 **`tempo_search`는 없다** — T2 트레이스 ID를 Tempo에서
      직접 확보해 `evidence/`에 남길 것. 안 남기면 채점 근거가 재현 불가가 된다.
- [ ] **baseline T2 트레이스도 확보.** "client span이 없다(캐시 히트) → 있다(직행)"의 대조가
      이 문항의 핵심이다. AU-2에서 baseline·symptom 트레이스가 65 spans로 동일했던 것과
      정확히 반대 그림이 나와야 한다.
- [ ] **관측 지연을 감안할 것.** AU-2 실측: 메트릭 마지막 스크레이프가 주입 1분 뒤(graceful
      shutdown 잔여), 신 파드 첫 스크레이프가 원복 3분 뒤(기동 지연). 관측되는 공백은 실제
      주입 창보다 넓고 뒤에 있다.
- [ ] **RCA 수집 창이 복구를 담는지 확인.** `RCA_WINDOW_PADDING_SECONDS=120`으로는 AU-2에서
      복구가 창 밖이었다(결함 7). 조사에 쓸 traceId는 **원복 이후 시점**의 것을 고르거나
      패딩을 늘릴 것.

## RCA 채점 (블라인드 — §8 루브릭)

채점 근거 원문은 rca-agent [`docs/scoring/README.md`](../../../../../yogurtte-rca-agent/docs/scoring/README.md).

| 항목 | 배점 | 1회 (v1, 갈래 A) | 2회 (v3) | 3회 (v3) |
|---|---|---|---|---|
| 근본 원인 정확도 | 40 | **40** | | |
| 근거 시그널 경로 | 30 | **N/A** | | |
| 오귀인 없음 | 20 | **20** | | |
| 조치 타당성 | 10 | **10** | | |
| **합계** | **100** | **산출 불가** (3항목 70/70) | | |

- 회차 1 산출 불가 사유: v1 앵커 근거경로 요건("3s timeout", "fallback 로그")이 **실현되지
  않는 것**이었다. 에이전트는 "타임아웃이 아니라 TCP RST"라고 **앵커를 정정**했다.
  → **앵커가 틀리고 에이전트가 맞은 첫 사례.**
- 평균 ± 최대편차: 산출 불가 (유효 채점 0회)
- 자기 일치도(§8.2): 미실시

## 실행 결과

### 회차 1 — 2026-07-27, 갈래 A (fallback 정상 저하)

- 주입 `07:00:51Z` → 원복 `07:23:42Z` (**22분 51초**), 로그인 200 복귀 확인 `07:26:06Z`
- baseline 채록 `07:00:29Z` · symptom 채록 `07:23:39Z` (**주입 후 22분 48초** = TTL 10분의 2.3배)

| 프로브 | baseline | symptom |
|---|---|---|
| 로그인 | 200 | **503** |
| T2 `?size=10` | 200 / 0.274902s | **200** / **0.170260s** |
| 익명 작성자 | 0 | **10명** |
| `user_fallback(1h)` | 0건 | 0건 ← **오보고**(아래) |
| `user_client_p99` | (없음) | (없음) |

**traceId**: baseline `6a67020d9d618589141817d961c25f9d` (74 spans, 232.11ms) /
symptom `6a67077c87b8b863f15cc6ee1ac95fbb` (66 spans, 126.61ms)

**핵심 실측 — trace 대조**

| | baseline | symptom |
|---|---|---|
| auth 호출 client span | `http get` 112.55ms 정상 | `http get` **23.55ms** `STATUS_CODE_ERROR` |
| auth 서버 span | `http get /external/users` 100.65ms | **없음** |

error 원문: `finishConnect(..) failed: Connection refused: auth-service.default.svc.cluster.local/10.43.13.21:8081`

**장애 중에 오히려 빨라졌다** (0.2749s → 0.1703s) — baseline은 auth 왕복 100ms를 기다렸고
symptom은 23.5ms만에 거절당했다. **지연 기반 알람으로는 원리적으로 못 잡는다.**

회차별 상세: rca-agent `docs/au-4/round-1.md` · 평가 `docs/findings/ae-06-rca-v0-au4-blind-eval.md`

### 발견된 계측 구멍 → 보강 (회차 2 전 적용 예정, 지금 고치지 말 것)

1. **fallback 로그가 `traceId=NONE`** — `.doOnError`가 Reactor 이벤트 루프에서 실행돼 MDC
   미전파. 로그는 있으나 traceId 기반 조사로 도달 불가.
   → rca-agent [NF-09](../../../../../yogurtte-rca-agent/docs/findings/nf-09-user-fallback-no-traceid.md)
2. **집계 지표 부재** — fallback 카운터 없음, `http_client_requests` 시리즈도 부재 → **알람 불가**
3. **로그 메시지 규약 부재** — `external/user` 패키지에 실패 로그 29개, 접두사 3종 혼용
   (없음 / `[외부사용자 조회]` / 이모지 `⚠️❌`). 안정적 쿼리가 성립할 수 없다
4. **하네스 프로브 정규식 불일치** — `loki_count user_fallback`이 `"대체 사용자|fallback|Fallback"`을
   찾는데 실제 메시지는 `"사용자 목록 조회 실패"`. **0건으로 오보고돼 "로그가 없다"고
   오판할 뻔했다** (실측으로 정정). → 접두사 기반으로 교체 필요
5. **`measure_AU_4`에 `tempo_search` 없음** — traceId를 수동 확보해야 했다
6. **DB 커넥션 점유 중 외부 HTTP 호출** — 실패한 `http get` span의 부모가 JDBC `connection`
   span. 이번엔 refused(23.5ms)라 무해했으나 auth가 *느려지면* 풀 고갈로 번진다.
   → **AU-1의 사전 가설**로 둔다.
   rca-agent [NF-10](../../../../../yogurtte-rca-agent/docs/findings/nf-10-content-db-connection-held-during-external-call.md)

변경 대기열과 전후 검증 프로토콜:
rca-agent [`docs/round-2/`](../../../../../yogurtte-rca-agent/docs/round-2/README.md)
