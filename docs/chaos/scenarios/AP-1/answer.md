# AP-1 — 댓글 201자: 검증 구멍 → varchar(200) 위반 → 500

> Then. Given/When은 [RUNBOOK.md](../../RUNBOOK.md) §6 AP-1. 여기엔 실행 결과를 남긴다.
> 인프라 무접촉 — 원복 없음(실패 INSERT는 롤백). **채록·채점 전에 결함을 고치지 말 것** (§6 AP 공통).

- **주입**: 250자 댓글 실요청 1건 (아무 셸, 공개 ingress)
- **hop**: 1
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

댓글 DTO(@NotBlank만, @Size 부재)가 201자를 통과시켜 varchar(200) INSERT에서 DataIntegrityViolation — DB가 아니라 앱 검증 구멍이 원인(+미매핑으로 400 아닌 500).

## 근거 시그널 도달 경로

핵심: 인프라 전부 정상 + 정상 댓글은 200인데 특정 요청만 500.
Loki "Data too long" → Tempo INSERT JDBC span error → 요청 페이로드 길이로 수렴.
단발 오류라 rate 기반 메트릭에는 거의 안 보임 — **로그·트레이스 없이는 도달 불가한 문항.**

1.
2.

## RCA 채점 (블라인드, RUNBOOK §8 루브릭)

| 항목 | 배점 | 획득 | 비고 |
|---|---|---|---|
| 근본 원인 정확도 | 40 | | "앱 검증 구멍"까지 도달해야 만점 — "DB 오류"에서 멈추면 미달 |
| 근거 시그널 경로 | 30 | | |
| 오귀인 없음 | 20 | | SQL 예외만 보고 "DB 장애"로 지목하면 감점 — DB는 제약을 지켰다 |
| 조치 타당성 | 10 | | @Size(max=200) 추가 + DataIntegrityViolation 매핑(400) 방향 |
| **합계** | **100** | | |

## evidence

- baseline: `evidence/baseline/<ts>/`
- symptom: `evidence/symptom/<ts>/`
- 채록 후 보강 커밋 (채점 전 수정 금지):
