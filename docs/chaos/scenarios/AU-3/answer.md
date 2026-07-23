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

## 채점 앵커 (채록 전 박제 — §8.2. 채록 후 수정 금지, 개정은 다음 회차부터)

| 항목 | 만점 | 부분점 | 0점 |
|---|---|---|---|
| 근본 원인 40 | 서비스 간 JWT 시크릿 불일치(config drift) — "로그인은 되는데 검증만 실패"의 구조까지 | 20: "인증 실패 급증"·"토큰 문제"까지만 | auth 다운·토큰 만료 등 지목 |
| 근거 경로 30 | 401 rate 급증 + JwtFilter 로그 + **trace에 안 잡힘(4xx)을 인지하고 metric/log로 도달** | 15: 메트릭만 | 근거 없음 |
| 오귀인 20 | 로그인 성공을 근거로 auth를 원인에서 배제 | — | auth 장애로 오진 |
| 조치 10 | 시크릿 재동기화 + 재배포 | 5: 재시작만 | 무관 조치 |

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
