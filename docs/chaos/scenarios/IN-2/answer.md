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

회차 1 실측(2026-07-26): 예외 삼킴 함정은 **불성립** — 현행 `NotificationEventListener` catch가 span error + traceId 실린 ERROR 로그를 남긴다. 실제 도달 경로는:

1. 트리거 트레이스 안: HTTP POST 54ms **200** 옆에 `notification-publish` **60,060ms STATUS_CODE_ERROR** + `publish user.notifications` 60,049ms ERROR — baseline(16ms OK + chat 소비 체인)과 대조하면 chat 쪽 span 전면 부재.
2. content 로그: `[Notification] 알림 발행 실패` ERROR 1건(트리거 +60.0s = `max.block.ms`), 원문에 `TimeoutException: Topic user.notifications not present in metadata after 60000 ms` — 재시도·DLQ 로그는 없음(발행측엔 그 경로 자체가 없음) → 유실 확정.
3. 보조: content producer NetworkClient WARN 스팸(traceId=NONE) / `kafka_brokers` 메트릭 **부재**(0이 아님) / chat 로그는 다운 내내 0건(소비자 침묵).

## 채점 앵커 (채록 전 박제 — §8.2. 채록 후 수정 금지, 개정은 다음 회차부터)

| 항목 | 만점 | 부분점 | 0점 |
|---|---|---|---|
| 근본 원인 40 | Kafka 다운 → AFTER_COMMIT 발행 실패로 **알림만 조용히 유실(재시도 없음), API는 정상** | 20: "알림 안 감"까지 (조용함·복구 불가 미도달) | content API·chat 지목 |
| 근거 경로 30 | baseline producer span 위치와 대조해 **span 부재/error를 근거로 사용** + 댓글 200 공존 | 15: 로그만 | 근거 없음 |
| 오귀인 20 | API 200을 "정상"으로 오판 안 함 | — | 오판 |
| 조치 10 | Kafka 복구 + 주입 중 유실분 복구 불가 인지 | 5: 복구만 | 무관 조치 |

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

- baseline: `evidence/baseline/20260726T091240Z/`(주입 전) · `evidence/baseline/20260726T092109Z/`(⑤ 복귀 확인 — 같은 쿼리 재실행)
- symptom: `evidence/symptom/20260726T091536Z/` (`trace-symptom-t1.json` = 60,060ms error span 원본, `loki-fail-lines.json` = ERROR 로그 원문)
- 주입 중 유실분(복구 불가) 기록: **정확히 1건** — 09:15:36Z 트리거 댓글의 알림. Mongo `user_notifications` 주입 창(09:14:23~09:19:41) 내 문서 0건 + 창 내 `알림 발행 실패` 로그 1건(=트리거분)으로 **실사용자 유실 0건** 교차 확인. 댓글 본문은 MySQL에 정상 저장(200) — 알림만 증발.
- 발견된 계측 구멍 → 보강 커밋: 회차 1 기준 전부 미수정 — chat 프로덕션 dev 프로필 기동, 소비자 브로커 다운 침묵(`org.apache.kafka=ERROR`), `kafka_brokers` absent 알람 부재, notificationExecutor CallerRunsPolicy 잠복 위험. 상세는 [RESULTS.md](../../RESULTS.md) IN-2 절.
