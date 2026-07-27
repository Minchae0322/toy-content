# AP-2 — 팔로우 목록: 선택 파라미터 미기본값 → `size+1` 언박싱 NPE → 500 (read-path, DB 미진입)

> Then. Given/When은 [RUNBOOK.md](../../RUNBOOK.md) §6 AP-2. 여기엔 실행 결과를 남긴다.
> 구 AP-2(대용량 업로드)는 폐기 — 실 업로드는 toy-auth presigned+S3라 content 로컬 경로가 비대표였다.
> 대체 결함은 순회에서 잡힌 자연발생 500(DF-01 #2)을 근인 확정한 것. 인프라 무접촉 · 원복 없음.

- **주입**: `size` 파라미터를 생략한 실요청 2건 — `GET /auth/user/1/following`, `GET /auth/user/1/followers`
- **hop**: 1 (auth-service 단일)
- **실행 일시(UTC)**: `evidence/timeline.log`

## 정답지 (원인 1줄)

`size`는 선택 파라미터(문서상 기본 20)인데 실제 기본값이 적용되지 않아 미지정 시 `Integer size=null`. `FollowCondition.limit()`의 `return size + 1;` 언박싱에서 NPE → 500. **DB 무관** — QueryDSL 쿼리 빌드 단계(`.limit(...)`)에서 터져 SQL은 실행되지 않았다. following/followers 양방향 동일 근원. `@Positive`/`@Max`는 null을 통과시켜 `@Valid`로도 안 막힌다.

**코드 근거**(정답지 — 블라인드 대상 아님):
- `toy-auth-user-region/.../user/controller/dto/FollowCondition.java:19-22`(size = `Integer`, `@NotNull` 없음, `@Schema(defaultValue="20")`는 문서값), `:24-25`·`:44-45`(`limit()` = `size + 1`)
- `.../follow/repository/querydsl/impl/FollowRepositoryCustomImpl.java:35`·`:65`(`.limit(followingSearch.limit())` — 쿼리 빌드 중 호출)

## 근거 시그널 도달 경로

핵심: **DB span의 부재를 근거로 읽는다.** auth-service가 500인데 error가 쿼리 실행 전(빌드 단계)에서 나므로 SQL span·DB 로그가 없다. "인프라 정상인데 왜 이 요청만" + "size=20이면 200 공존" → 요청 파라미터로 시선. 두 엔드포인트 동일 500 → 단일 근원.

1.
2.

## 채점 앵커 (⚠️ DRAFT — 실측 채록 전까지 무효. baseline 채록을 입력으로 박제 후 유효)

> **아직 박제되지 않았다.** 아래는 코드 독해 기반 초안이다. AU-4 교훈(코드 상수 ≠ 실측)대로,
> `evidence/AP-2/baseline/`·`symptom/` 채록에서 **실제로 관측된 신호로만** 만점 요건을 확정한 뒤
> 이 배너를 지우고 "채록 전 박제"로 전환한다. 확정 전 채점은 §8.2상 무효.
>
> **채록 시 실측 확인 항목** (만점 요건 후보의 존재 검증):
> - Tempo: 500 error span이 auth-service 어디에 찍히나? **DB/SQL span이 정말 없나?** 예외 클래스(`NullPointerException`)·스택이 span 태그로 붙나?
> - Loki: NPE 앱 로그가 남나, 아니면 `GlobalExceptionHandler`가 삼켜 스택이 안 남나(NF-09처럼 `traceId=NONE`일 수도)? — **로그가 없으면 근거경로 요건은 트레이스로만 구성.**
> - 스택 최상단이 실제로 `FollowCondition$…limit()`(record accessor)로 뜨나?
> - 위 신호가 **에이전트 수집 쿼리(트레이스/로그 채널)에 실제로 들어오나** (B유형 앵커부적합 방지).

| 항목 | 만점 (초안) | 부분점 | 0점 |
|---|---|---|---|
| 근본 원인 40 | `size` 미기본값 → `limit()`의 `size+1` 언박싱 NPE 특정 + **DB 무관(SQL 미실행)** 명시 | 20: NPE까지만, 미기본값 근인 미도달 | DB/커넥션/데이터 문제로 지목 |
| 근거 경로 30 | error 트레이스 위치(DB span 부재) + (실측상 존재 시)NPE 스택의 `limit()` 지목 | 15: 500 코드만 | 근거 없음 |
| 오귀인 20 | DB·커넥션·데이터를 원인으로 오진하지 않음 | — | 오진 |
| 조치 10 | 실기본값 적용(`size==null?20:size`) 또는 primitive+default | 5: `@Valid`/`@NotNull`만(null이 `@Positive` 통과 — 부분) | 무관 조치 |

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

- baseline: `evidence/baseline/<ts>/` (게이트: `following?size=20` = 200)
- symptom: `evidence/symptom/<ts>/` (`following`·`followers` size 생략 = 500)
- 채록 후 앵커 박제 커밋(채점 전 수정 금지):
- 보강 커밋(채점 후 — 실기본값 적용):
