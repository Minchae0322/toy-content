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

## RCA 채점 (블라인드, RUNBOOK §8 루브릭)

| 항목 | 배점 | 획득 | 비고 |
|---|---|---|---|
| 근본 원인 정확도 | 40 | | |
| 근거 시그널 경로 | 30 | | content trace 무결 → lag로 지목했는가 |
| 오귀인 없음 | 20 | | |
| 조치 타당성 | 10 | | |
| **합계** | **100** | | |

## evidence

- baseline: `evidence/baseline/<ts>/`
- symptom: `evidence/symptom/<ts>/`
- 발견된 계측 구멍 → 보강 커밋:
