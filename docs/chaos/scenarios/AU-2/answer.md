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

## 채점 앵커 (채록 전 박제 — §8.2. 채록 후 수정 금지, 개정은 다음 회차부터)

| 항목 | 만점 | 부분점 | 0점 |
|---|---|---|---|
| 근본 원인 40 | auth 프로세스 전면 부재(다운) + content 증상(미스분 익명)과의 연결 | 20: "auth 이상"까지만 | content·ingress 자체 장애로 지목 |
| 근거 경로 30 | connection refused **즉시 실패**(3s timeout 아님 — AU-1과 구별) + 로그인 502의 출처(ingress) | 15: 502만 인용 | 근거 없음 |
| 오귀인 20 | content를 원인으로 지목하지 않음 (캐시 히트분 정상을 근거로) | — | content 장애로 오진 |
| 조치 10 | auth 재기동(replicas 복원) | 5: 방향은 맞으나 대상 서비스 특정 실패 | 무관 조치 |

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
