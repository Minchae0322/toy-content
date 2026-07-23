# IN-1 — Redis 다운 (다중 서비스 복합, 최고 난도)

> Then. Given/When은 [RUNBOOK.md](../../RUNBOOK.md) §6 IN-1. 여기엔 실행 결과를 남긴다.

- **주입**: `docker stop $REDIS_CT` (infra 노드)
- **hop**: 다중
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

Redis 다운. 캐시·분산락·프레즌스·인증코드가 한꺼번에 무너지되 서비스별 증상이 다름.

## 근거 시그널 도달 경로

핵심: 서비스별로 다르게 아픈 증상을 **단일 근원(Redis)으로 수렴**시키는 능력이 채점 포인트.
- content: user 캐시 미스 → auth 직행 호출 급증 + ShedLock 실패 → 스케줄러 skip → 핫스코어 갱신 정체
- chat: 프레즌스 조회 실패 → WS/FCM 이상
- auth: 이메일 인증·위치 검색 실패

1.
2.

## RCA 채점 (블라인드, RUNBOOK §8 루브릭)

| 항목 | 배점 | 획득 | 비고 |
|---|---|---|---|
| 근본 원인 정확도 | 40 | | |
| 근거 시그널 경로 | 30 | | 3개 서비스 증상을 하나로 수렴했는가 |
| 오귀인 없음 | 20 | | |
| 조치 타당성 | 10 | | |
| **합계** | **100** | | |

## evidence

- baseline: `evidence/baseline/<ts>/`
- symptom: `evidence/symptom/<ts>/`
- 발견된 계측 구멍 → 보강 커밋:
