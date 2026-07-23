# AU-2 — auth 완전 다운

> Then. Given/When(전제·주입·판정 기준)은 [RUNBOOK.md](../../RUNBOOK.md) §6 AU-2. 여기엔 실행 결과를 남긴다.

- **주입**: `kubectl scale deploy/$AUTH_DEPLOY --replicas=0`
- **hop**: 2
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

auth 전면 다운. content는 캐시 히트분 정상 + 미스분 익명 — fallback 설계 검증.

## 근거 시그널 도달 경로

채록하며 채운다 — 어떤 trace/metric/log를 어떤 순서로 봐야 원인 1줄에 도달하는가.
핵심: content client span이 **timeout(3s)이 아니라 connection refused로 즉시 실패** (AU-1과 구별).

1.
2.

## RCA 채점 (블라인드, RUNBOOK §8 루브릭)

| 항목 | 배점 | 획득 | 비고 |
|---|---|---|---|
| 근본 원인 정확도 | 40 | | |
| 근거 시그널 경로 | 30 | | |
| 오귀인 없음 | 20 | | |
| 조치 타당성 | 10 | | |
| **합계** | **100** | | |

## evidence

- baseline: `evidence/baseline/<ts>/`
- symptom: `evidence/symptom/<ts>/`
- 발견된 계측 구멍 → 보강 커밋:
