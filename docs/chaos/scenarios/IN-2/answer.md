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

## 채점 앵커 — **v2 (2026-07-27 개정, 회차 2부터 적용)**

> §8.2에 따라 개정 앵커는 **다음 회차부터** 적용한다. 이미 채록된 회차 1은 아래 v1으로
> 채점했고(80/100) 그 점수는 그대로 둔다. 개정 사유는 rca-agent
> `docs/scoring/README.md` "앵커 결함" 절.

**영향 판정 용어**: "유실"이 아니라 **"10분 내 미도착"**으로 판정한다(기준 시각 = 트리거 발사).
IN-2는 발행측에 재시도·아웃박스 경로가 없어 사실상 영구 미도착이지만, 판정 문구는 관측
가능한 기준으로 통일한다 — CH-1과 같은 자를 써야 두 문항의 영향 판정 능력을 비교할 수 있다.

| 항목 | 만점 | 부분점(상) | 부분점(하) | 0점 |
|---|---|---|---|---|
| 근본 원인 40 | **Kafka 브로커 다운** → AFTER_COMMIT 발행 실패 → 알림만 조용히 **10분 내 미도착**(발행측 재시도 경로 없음), API는 정상 | 30: 발행 실패·미도착·API 정상은 정확하나 **하위 원인(브로커 다운)을 1순위로 지목 실패** | 20: "알림 안 감"까지 (조용함·재시도 부재 미도달) / 미도착 여부 **오판** | content API·chat 지목 |
| 근거 경로 30 | producer span error(60,060ms를 `max.block.ms` 기본값과 대조) + **chat span 부재를 소비측 배제 근거로 사용** + 댓글 200 공존 | 20: span error는 썼으나 **부재 신호 미활용** | 10: 로그만 | 근거 없음 |
| 오귀인 20 | API 200을 "정상"으로 오판 안 함 + chat을 원인으로 지목 안 함 | 10: 둘 중 하나만 | — | 오판 |
| 조치 10 | Kafka 복구 + 미도착분 복구 불가 인지 | 5: 복구만 | — | 무관 조치 |

> **v1에서 제거한 요건**: 근거 경로 만점의 "**baseline** producer span 위치와 대조". §8.2
> 입력 패키징 ①은 baseline 창을 함께 주라고 하지만 v0는 traceId 1개만 받는다 — 에이전트가
> 구조적으로 달성할 수 없는 요건이라 만점 기준에서 뺐다. baseline 대조를 다시 요구하려면
> 입력 설계부터 바꿔야 한다(v0.1).
>
> **참고(적용 불가)**: 회차 1을 v2로 재채점하면 근본 원인이 20 → 30이 되어 **90**이다.
> §8.2상 재채점은 금지이므로 공식 점수는 80으로 둔다. 이 10점 차가 앵커 결함 2(2단계
> 앵커라 부분 충족을 표현 못 함)의 실측 크기다.

### v1 앵커 (회차 1에 적용된 원본 — 보존)

| 항목 | 만점 | 부분점 | 0점 |
|---|---|---|---|
| 근본 원인 40 | Kafka 다운 → AFTER_COMMIT 발행 실패로 **알림만 조용히 유실(재시도 없음), API는 정상** | 20: "알림 안 감"까지 (조용함·복구 불가 미도달) | content API·chat 지목 |
| 근거 경로 30 | baseline producer span 위치와 대조해 **span 부재/error를 근거로 사용** + 댓글 200 공존 | 15: 로그만 | 근거 없음 |
| 오귀인 20 | API 200을 "정상"으로 오판 안 함 | — | 오판 |
| 조치 10 | Kafka 복구 + 주입 중 유실분 복구 불가 인지 | 5: 복구만 | 무관 조치 |

## RCA 채점 (블라인드 — §8 루브릭, 회차는 §8.1 반복 프로토콜)

채점 근거 원문은 rca-agent [`docs/scoring/README.md`](../../../../../yogurtte-rca-agent/docs/scoring/README.md).

| 항목 | 배점 | 1회 (v1) | 2회 (v2) | 3회 (v2) |
|---|---|---|---|---|
| 근본 원인 정확도 | 40 | 20 | | |
| 근거 시그널 경로 | 30 | 30 | | |
| 오귀인 없음 | 20 | 20 | | |
| 조치 타당성 | 10 | 10 | | |
| **합계** | **100** | **80** | | |

- 회차 1 감점 20 전액: 하위 원인 감별 실패 — 1순위를 "토픽 부재"로, 실제(브로커 전면 다운)를
  2순위·확신도 낮음~중간에 뒀다. `peer.service`의 클러스터 ID(다운 전 캐시값)를 "연결 성립"
  증거로 오독한 것이 핵심.
- 평균 ± 최대편차: **산출 불가 (N=1)** — §8.1상 인용 불가. 회차 2 필요.
- 자기 일치도(동일 출력 2회 채점 차, ±5 초과 시 앵커 보강 — §8.2): **미실시**

## evidence

- baseline: `evidence/baseline/20260726T091240Z/`(주입 전) · `evidence/baseline/20260726T092109Z/`(⑤ 복귀 확인 — 같은 쿼리 재실행)
- symptom: `evidence/symptom/20260726T091536Z/` (`trace-symptom-t1.json` = 60,060ms error span 원본, `loki-fail-lines.json` = ERROR 로그 원문)
- 주입 중 유실분(복구 불가) 기록: **정확히 1건** — 09:15:36Z 트리거 댓글의 알림. Mongo `user_notifications` 주입 창(09:14:23~09:19:41) 내 문서 0건 + 창 내 `알림 발행 실패` 로그 1건(=트리거분)으로 **실사용자 유실 0건** 교차 확인. 댓글 본문은 MySQL에 정상 저장(200) — 알림만 증발.
- 발견된 계측 구멍 → 보강 커밋: 회차 1 기준 전부 미수정 — chat 프로덕션 dev 프로필 기동, 소비자 브로커 다운 침묵(`org.apache.kafka=ERROR`), `kafka_brokers` absent 알람 부재, notificationExecutor CallerRunsPolicy 잠복 위험. 상세는 [RESULTS.md](../../RESULTS.md) IN-2 절.
