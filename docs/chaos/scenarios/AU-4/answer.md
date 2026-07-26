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

## 채점 앵커 (채록 전 박제 — §8.2. 채록 후 수정 금지, 개정은 다음 회차부터)

| 항목 | 만점 | 부분점 | 0점 |
|---|---|---|---|
| 근본 원인 40 | auth 전면 다운 + 캐시 만료로 fallback 발동(익명)까지 연결 | 20: "auth 이상"까지만 | content·Redis 자체 장애로 지목 |
| 근거 경로 30 | user client span timeout(3s) → fallback 로그 → 작성자 익명 순으로 도달 | 15: 일부 시그널만 | trace만 보고 원인 추정 |
| 오귀인 없음 20 | content/Redis를 원인으로 지목 안 함 (fallback은 정상 저하) | — | 피드 500을 auth 탓으로(AU-2 오염 재현) |
| 조치 타당성 10 | auth 복구 방향 + 캐시 워밍/TTL 검토 | — | 무관한 조치 |

## 실행 결과

미실행 — 첫 회차 대기. auth 다운 10분+ 유지가 필요해 저트래픽 시간대 권장.
