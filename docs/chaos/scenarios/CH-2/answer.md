# CH-2 — 컨슈머 정지 → lag 누적

> Then. Given/When은 [RUNBOOK.md](../../RUNBOOK.md) §6 CH-2. 여기엔 실행 결과를 남긴다.
> **전제: chat consumer lag 메트릭 노출(§10).** 미충족 시 `baseline` 게이트에서 exit 1 = **보류**.

- **주입**: `kubectl scale deploy/$CHAT_DEPLOY --replicas=0`
- **hop**: 2
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

chat 컨슈머 전멸로 lag 누적. 발행측 정상, 복구 시 밀린 알림 일괄 처리.

## 근거 시그널 도달 경로

핵심: content 트레이스는 **깨끗해야 한다** — "발행은 됐는데 소비가 없다"를 lag로 지목하는 게 채점 포인트.
`kafka_consumer_fetch_manager_records_lag_max` 누적 곡선 + `websocket_active_users` 급락으로 도달.

1.
2.

## 채점 앵커 (채록 전 박제 — §8.2. 채록 후 수정 금지, 개정은 다음 회차부터)

| 항목 | 만점 | 부분점 | 0점 |
|---|---|---|---|
| 근본 원인 40 | 컨슈머 전멸로 lag 누적 — **발행측 정상 판단 포함** | 20: "알림 지연"까지만 | content 발행측·브로커 지목 |
| 근거 경로 30 | lag 누적 곡선 + active_users 급락 + **content 트레이스 무결** 3종 연결 | 15: lag만 | 근거 없음 |
| 오귀인 20 | content·Kafka 브로커를 원인에서 배제 | — | 지목 |
| 조치 10 | 컨슈머 복구 + 밀린 알림 일괄 도착 예고 | 5: 복구만 | 무관 조치 |

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
