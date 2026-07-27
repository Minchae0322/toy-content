# AU-4 — auth 완전 다운 + user 캐시 만료 (fallback 경로 실검증)

> Then. Given/When(전제·주입·판정 기준)은 [RUNBOOK.md](../../RUNBOOK.md) §6 AU-4. 여기엔 실행 결과를 남긴다.
> AU-2에서 파생 신설(2026-07-26): AU-2는 캐시 히트라 content 무영향(실명 유지)이라, "auth 죽고 캐시도 없을 때"의 fallback 경로가 검증되지 않았다. 그 구멍을 메우는 문항.

- **주입**: `kubectl scale deploy/$AUTH_DEPLOY --replicas=0` (AU-2와 동일) + **10분+ 유지**(캐시 TTL 경과 대기)
- **hop**: 2~3 (content → user 캐시 미스 → auth 직행 실패 → fallback)
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

auth 전면 다운 상태에서 user 캐시(TTL 10분)까지 만료 → content가 auth 직행 → 3s timeout → `createFallbackUserInfo`(익명 "사용자N")로 저하. **원인은 auth 다운이고, content 500이면 fallback 붕괴(별개 실버그)**.

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
핵심: T2(`?size=10`) 응답의 작성자 nickname이 "사용자N" 익명인가 — `ExternalUserApiClient` 3s timeout(`Duration.ofSeconds(3)`) 후 `createFallbackUserInfo` 경로. 캐시 만료 전(10분 내)에는 실명이라 AU-2와 구별 안 됨.

## 전제 (§10)

- **user 캐시 TTL = 10분** (`UserCacheStore.DEFAULT_TTL = Duration.ofMinutes(10)`). 이보다 짧게 주입하면 캐시 히트라 AU-2와 동일 — 반드시 10분+ 유지.
- content fallback 코드 존재 확인됨: `ExternalUserInfoService.getUserInfo`가 "절대 null 반환 안 함", API 실패 시 `createFallbackUserInfo`. **이 fallback이 실제로 버티는지가 검증 대상.**
- `/feeds/scroll`은 `?size=` 필수 (미지정 시 NPE 500 — content `1e7df3f`로 수정, 그 전 chaos.sh t2는 size 없이 호출해 오염됐음).

## 채점 앵커 — **v2 (2026-07-27 작성, 채록 전 박제 — §8.2)**

> **이 문항은 v2 원칙으로 처음부터 작성된 첫 앵커다.** 미실행 상태에서 박제하므로 회차 1부터
> 적용된다(CH-1·CH-2·IN-2·AU-2는 v1으로 채록된 회차가 있어 v2가 다음 회차부터였다).
> v2 원칙과 그 근거는 rca-agent `docs/scoring/README.md` "앵커 결함" 절.

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
| 근거 경로 30 | `GET user-service` client span의 **3s timeout**(`ExternalUserApiClient`의 `Duration.ofSeconds(3)`와 대조) + 작성자 익명 전환 + fallback 로그 | 20: 3종 중 2종 / 또는 **동등한 대체 경로로 도달**(예: auth 메트릭 시계열 소멸을 대조군과 함께 제시) | 10: 작성자 익명만 인용 | 근거 없음 |
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

## 실행 결과

미실행 — 첫 회차 대기. auth 다운 **10분+** 유지가 필요해 저트래픽 시간대 권장.
