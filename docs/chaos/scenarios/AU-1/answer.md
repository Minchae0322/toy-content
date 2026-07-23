# AU-1 — auth CPU 기아 → 조용한 성능·데이터 품질 저하

> Then. Given/When은 [RUNBOOK.md](../../RUNBOOK.md) §6 AU-1. 여기엔 실행 결과를 남긴다.

- **주입**: cpu limit 50m patch (`kubectl patch deploy/$AUTH_DEPLOY`)
- **hop**: 2~3
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

auth CPU limit 과소로 응답 지연. content는 3s timeout + fallback으로 방어했으나 데이터 품질 저하.

## 근거 시그널 도달 경로

핵심 함정: **에러율(5xx)은 오르지 않는다** — 메트릭만 보는 RCA는 여기서 감점.
client span이 정확히 3s에서 잘리는 트레이스 + fallback 로그 + 작성자 익명 표시로 도달.

1.
2.

## RCA 채점 (블라인드, RUNBOOK §8 루브릭)

| 항목 | 배점 | 획득 | 비고 |
|---|---|---|---|
| 근본 원인 정확도 | 40 | | |
| 근거 시그널 경로 | 30 | | |
| 오귀인 없음 | 20 | | 5xx 무변화 함정에 안 빠졌는가 |
| 조치 타당성 | 10 | | |
| **합계** | **100** | | |

## evidence

- baseline: `evidence/baseline/<ts>/`
- symptom: `evidence/symptom/<ts>/`
- 발견된 계측 구멍 → 보강 커밋:
