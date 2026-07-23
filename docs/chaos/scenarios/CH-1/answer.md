# CH-1 — Mongo 다운 → 컨슈머 재시도 → DLQ

> Then. Given/When은 [RUNBOOK.md](../../RUNBOOK.md) §6 CH-1. 여기엔 실행 결과를 남긴다.

- **주입**: `docker stop $MONGO_CT` (infra 노드)
- **hop**: 2
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

MongoDB 다운. 컨슈머는 3회 재시도 후 DLQ 적재 — 메시지 유실은 없음.

## 근거 시그널 도달 경로

핵심: 댓글 API는 **200** (발행은 AFTER_COMMIT, 실패는 컨슈머 쪽).
chat 트레이스의 FixedBackOff(1000ms × 3) 재시도 → DLQ producer span + Mongo span error 태그로 도달.

1.
2.

## 채점 앵커 (채록 전 박제 — §8.2. 채록 후 수정 금지, 개정은 다음 회차부터)

| 항목 | 만점 | 부분점 | 0점 |
|---|---|---|---|
| 근본 원인 40 | MongoDB 다운 → 컨슈머 실패 → 재시도 후 DLQ 적재, **유실 없음 판단 포함** | 20: "chat 컨슈머 실패"까지 (Mongo 미도달 또는 유실 여부 미판단) | Kafka·content 지목 |
| 근거 경로 30 | consume span 하위 Mongo 예외 → FixedBackOff 3회 → DLQ producer span을 **한 트레이스로** 연결 | 15: DLQ 로그만 | 근거 없음 |
| 오귀인 20 | 댓글 200을 "장애 없음"으로 오판 안 함 + Kafka를 지목 안 함 | — | 어느 한쪽 위반 |
| 조치 10 | Mongo 복구 + DLQ 적재분 처리 방침 언급 | 5: Mongo 복구만 | 무관 조치 |

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
- 복구 후: DLQ 적재분 처리 방침 (재발행 수단 유무) —
- 발견된 계측 구멍 → 보강 커밋:
