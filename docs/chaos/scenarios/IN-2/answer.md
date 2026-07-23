# IN-2 — Kafka 다운 (조용한 유실)

> Then. Given/When은 [RUNBOOK.md](../../RUNBOOK.md) §6 IN-2. 여기엔 실행 결과를 남긴다.
> 데이터 유실 동반 — 주입 전 알림 유실 허용 범위를 먼저 정한다.

- **주입**: `docker stop $KAFKA_CT` (infra 노드)
- **hop**: 2
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

Kafka 다운. AFTER_COMMIT 발행이 실패하며 알림만 조용히 유실 — API는 전부 정상.

## 근거 시그널 도달 경로

핵심: 댓글 200 + 알림 영구 미도착(재시도 없음).
`NotificationService.sendSafely`가 예외를 삼켜 **아무 span도 안 남으면** 그 자체가 계측 구멍 발견(§9) → 보강 후 재채록.
baseline에서 producer span 위치를 먼저 기록해야 "사라진 span"을 알아본다.

1.
2.

## RCA 채점 (블라인드, RUNBOOK §8 루브릭)

| 항목 | 배점 | 획득 | 비고 |
|---|---|---|---|
| 근본 원인 정확도 | 40 | | |
| 근거 시그널 경로 | 30 | | |
| 오귀인 없음 | 20 | | API 200을 정상으로 오판하지 않았는가 |
| 조치 타당성 | 10 | | |
| **합계** | **100** | | |

## evidence

- baseline: `evidence/baseline/<ts>/`
- symptom: `evidence/symptom/<ts>/`
- 주입 중 유실분(복구 불가) 기록:
- 발견된 계측 구멍 → 보강 커밋:
