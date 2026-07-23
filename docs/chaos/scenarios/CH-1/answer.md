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

## RCA 채점 (블라인드, RUNBOOK §8 루브릭)

| 항목 | 배점 | 획득 | 비고 |
|---|---|---|---|
| 근본 원인 정확도 | 40 | | |
| 근거 시그널 경로 | 30 | | |
| 오귀인 없음 | 20 | | 댓글 200을 정상으로 오판하지 않았는가 |
| 조치 타당성 | 10 | | |
| **합계** | **100** | | |

## evidence

- baseline: `evidence/baseline/<ts>/`
- symptom: `evidence/symptom/<ts>/`
- 복구 후: DLQ 적재분 처리 방침 (재발행 수단 유무) —
- 발견된 계측 구멍 → 보강 커밋:
