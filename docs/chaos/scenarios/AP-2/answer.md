# AP-2 — 첨부 대용량 업로드: 실패 계층 판별 (ingress vs multipart vs 앱)

> Then. Given/When은 [RUNBOOK.md](../../RUNBOOK.md) §6 AP-2. 여기엔 실행 결과를 남긴다.
> 대역폭 부하 — 저트래픽 시간대·1회만. baseline(1KB)이 500이면 잠복 버그(경로 구분자) 실증 = 문항 이전에 실버그 발견으로 여기 기록하고 중단.

- **주입**: 대용량 파일 업로드 실요청 (2MB → 통과 시 1100MB, `AP2_MB`로 조절)
- **hop**: 1~2 (계층 판별)
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

업로드 실패는 계층 문제 — 앱 시그널이 없으면 ingress body limit, 있으면 Spring multipart 한도 + MaxUploadSizeExceededException 미매핑(413 아닌 500). 근본 원인은 앱 레벨 크기·타입 검증 부재.

## 근거 시그널 도달 경로

핵심: **응답 본문의 출처**(nginx HTML vs 앱 JSON)와 **앱 시그널 유무**(Loki·Tempo)의 조합으로 거부 계층을 특정.
앱 로그·트레이스가 전무한 4xx = 요청이 앱에 도달하지 않았다는 증거 — "시그널 부재를 읽는" 문항.

1.
2.

## RCA 채점 (블라인드, RUNBOOK §8 루브릭)

| 항목 | 배점 | 획득 | 비고 |
|---|---|---|---|
| 근본 원인 정확도 | 40 | | 거부 계층을 정확히 지목했는가 (ingress / multipart / 앱) |
| 근거 시그널 경로 | 30 | | 시그널 "부재"를 근거로 썼는가 |
| 오귀인 없음 | 20 | | ingress 차단을 앱 장애로 오진하지 않았는가 |
| 조치 타당성 | 10 | | 크기·MIME 검증 추가 + 413 매핑 방향 |
| **합계** | **100** | | |

## evidence

- baseline: `evidence/baseline/<ts>/`
- symptom: `evidence/symptom/<ts>/`
- 성공 업로드분(200) fileId·서버 파일 정리 기록:
- 채록 후 보강 커밋 (채점 전 수정 금지):
