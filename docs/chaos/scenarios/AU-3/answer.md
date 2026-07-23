# AU-3 — JWT 시크릿 드리프트 (config drift)

> Then. Given/When은 [RUNBOOK.md](../../RUNBOOK.md) §6 AU-3. 여기엔 실행 결과를 남긴다.

- **주입**: secret `JWT_SECRET` 변경 + content rollout restart
- **hop**: 1
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

content의 JWT_SECRET이 auth와 어긋남(config drift). 로그인은 되는데 아무것도 안 되는 상태.

## 근거 시그널 도달 경로

핵심: **401은 4xx라 span error 태그가 안 붙는다** — trace로는 못 찾는 문항.
401 rate 메트릭 + `{application="content-service"} |= "JwtFilter"` 로그로 도달.

1.
2.

## RCA 채점 (블라인드, RUNBOOK §8 루브릭)

| 항목 | 배점 | 획득 | 비고 |
|---|---|---|---|
| 근본 원인 정확도 | 40 | | |
| 근거 시그널 경로 | 30 | | trace 부재 → metric/log로 도달했는가 |
| 오귀인 없음 | 20 | | |
| 조치 타당성 | 10 | | |
| **합계** | **100** | | |

## evidence

- baseline: `evidence/baseline/<ts>/`
- symptom: `evidence/symptom/<ts>/`
- 발견된 계측 구멍 → 보강 커밋:
