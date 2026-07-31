# 로그 포맷 통합 규칙 — 저장은 기계용 한 줄, 사람용은 표시 층에서

toy-content · toy-chat · toy-auth 세 서비스의 로그 기록 형식을 통합하는 규칙.
독자가 둘이다 — **rca-agent(LogQL 쿼리)** 와 **사람(Grafana·로컬 콘솔)**.
둘을 한 형식으로 만족시키려 하지 않는다. **저장 형식은 기계 기준으로 하나로 통일하고,
사람이 보기 좋게 만드는 일은 표시 층(Grafana `line_format` · 로컬 프로파일)에 맡긴다.**

작성 2026-07-31. 적용은 아직 안 됐다 — [§6 적용·검증](#6-적용검증-절차)의 상태 표가 SoT.

---

## 1. 왜 통합하나 — 실측된 결함 4개 + 서비스 간 격차

전부 rca-agent 조사에서 실측으로 확인된 것이다 (rca-agent `CollectProperties` javadoc ·
rca-agent CLAUDE.md §7).

| # | 결함 | 원인 | 현재 상태 |
|---|---|---|---|
| 1 | `\| logfmt \| level=~` 쿼리가 항상 0건 | 평문 Logback이라 `level` 필드가 안 만들어짐 | 라인 필터 `\|~ "ERROR\|WARN"`로 우회 중 — 본문에 ERROR가 든 일반 줄까지 잡는 과대 매칭 |
| 2 | 스택트레이스가 traceId 쿼리에 안 잡힘 (B-11) | Logback 패턴은 이벤트 첫 줄에만 적용되고, 수집기가 줄 단위로 받아 스택이 traceId 없는 별개 엔트리가 됨 | `Exception\|Caused by\|\.java:[0-9]+\)` 정규식으로 우회 중 |
| 3 | `count_over_time` 집계 부풀림 | 스택 한 번에 수십 줄 → 발생 1건이 수십 건으로 집계 | 스윕 쿼리에서 스택 패턴을 빼는 것으로 우회 중 |
| 4 | ANSI 색코드가 파서를 깨뜨림 | 부트 기본 콘솔 패턴의 `%clr` | 레벨 위치 정규식 고정 불가 |

네 결함의 공통 원인이 하나다 — **"로그 이벤트 하나가 평문 여러 줄로 저장된다"**.
우회가 아니라 원인을 없애려면 저장 단위를 이벤트 단위로 바꿔야 한다.

여기에 서비스 간 격차가 있다 (2026-07-31 레포 실측):

| 서비스 | Boot | `pattern.level` (traceId MDC) | 콘솔 패턴 | 트레이싱 의존성 |
|---|---|---|---|---|
| toy-content | 3.4.1 | 있음 | 평문 명시 (prod) | 있음 |
| toy-chat | 3.5.3 | 있음 | **부트 기본값** (`%clr` ANSI) | 있음 |
| auth (toy-user) | 3.2.1 | 있음 | **`%clr` ANSI 명시** | 있음 |

같은 값(`%5p [traceId=...,spanId=...,userId=...]`)을 레포마다 복붙해 둔 상태다.
규칙이 없으면 이렇게 갈라진다.

> **정정 (2026-07-31).** 이 표의 auth 행은 처음에 `toy-auth` 레포(트레이싱·패턴 전무)를 보고
> 작성됐는데, **그 레포는 구본이었다.** auth-service의 현행 코드는 `toy-user`이고
> (`spring.application.name: auth-service` · 트레이싱 의존성·traceId 패턴 있음),
> A-0(JWT 예외 포착)가 적용된 `toy-auth-user-region`도 같은 서비스의 레포다 —
> **auth 레포가 둘 존재하며 어느 쪽이 배포본인지 확정이 필요하다.**

---

## 2. 원칙

> **수집 경로에 실리는 로그는 "JSON 한 줄 = 이벤트 한 개"다.
> 사람용 보기 좋음은 저장 형식이 아니라 표시 층에서 만든다.**

- 기계(rca-agent): `| json` 한 번으로 `level`·`traceId`·`stack_trace`가 필드가 된다.
  라인 필터 우회 4종이 전부 필요 없어진다.
- 사람(Grafana): `line_format`으로 원하는 모양으로 렌더한다 — 저장이 JSON이어도
  화면은 평문보다 깔끔하게 만들 수 있다.
- 사람(로컬 개발): 로컬 프로파일만 평문 콘솔을 유지한다. 수집 경로가 아니므로 규칙 밖이다.

---

## 3. 규칙

### R1 — 수집 경로 포맷은 JSON 한 줄, 부트 내장 structured logging을 쓴다

```yaml
# prod(=수집 경로) 프로파일
logging:
  structured:
    format:
      console: logstash
```

- 커스텀 logback.xml을 만들지 않는다 — 세 레포에 XML을 복붙하면 반드시 갈라진다.
  부트 내장(3.4+)이면 설정 한 줄이라 갈라질 표면적이 없다.
- 포맷은 `ecs`가 아니라 **`logstash`** 다. 이유:
  - 키가 평평하다. ECS는 `log.level`처럼 중첩이라 LogQL `| json` 후 `log_level`로
    이름이 바뀌어, 문서·쿼리·사람 머릿속 이름이 셋으로 갈라진다.
  - MDC(`traceId` 등)가 최상위 키로 나온다 — 쿼리가 `| json | traceId="..."`로 끝난다.
  - 키가 짧아 토큰을 덜 쓴다 (이 프로젝트의 개선 지표가 컨텍스트 토큰이다).

### R2 — 필드 이름은 박제한다. 바꾸지 않는다

| 필드 | 값 | 출처 |
|---|---|---|
| `@timestamp` | ISO-8601 | 포맷 기본 |
| `level` | `ERROR` / `WARN` / `INFO` / `DEBUG` | 포맷 기본 |
| `message` | 사람 문장 (R7) | 포맷 기본 |
| `logger_name` · `thread_name` | 클래스명 · 스레드 | 포맷 기본 |
| `stack_trace` | 예외 전문 (R3) | 포맷 기본 |
| `traceId` · `spanId` · `userId` | MDC — **기존 키 그대로, camelCase** | 앱 |

- `trace_id`로 "정리"하지 않는다. 기존 MDC 키·기존 문서·rca-agent 쿼리가 전부
  `traceId`로 박제돼 있고, 이름을 바꾸면 그 전부가 조용히 0건이 된다 —
  이 프로젝트에서 라벨 이름 하나(`app` vs `service_name`)로 조사 6회가 빈 결과였다.

### R3 — 예외는 같은 이벤트의 `stack_trace` 필드로 실린다

- 예외를 남길 때는 `log.error("...", e)` 형태만 쓴다.
  `e.printStackTrace()`, `log.error(e.getMessage())`(스택 소실), 개행 포함 메시지 금지.
- 이것이 결함 #2(B-11)·#3의 해소 지점이다: 스택이 traceId를 가진 이벤트 **안에** 있으므로
  traceId 쿼리로 스택 전문에 도달하고, 집계는 이벤트 1건으로 센다.

### R4 — 서비스 식별은 Loki 라벨 `service_name`이 유일한 SoT다

- Alloy가 붙이는 `service_name`(`content-service` 등)을 쓴다. 본문(JSON)에
  서비스명 필드를 또 넣지 않는다 — SoT가 둘이 되고 모든 줄에서 토큰을 낭비한다.
- `application` 라벨은 메트릭 전용이고 Loki에 없다 (observability.md 2026-07-25).

### R5 — 수집 경로에 ANSI 색코드 금지

- JSON 포맷이면 자연히 없어지지만, 명시적으로 `spring.output.ansi.enabled: never`를
  prod 프로파일에 둔다. 색은 로컬 콘솔(사람 전용 경로)에만 허용된다.

### R6 — 사람용 렌더는 표시 층에서. 표준 쿼리를 박제한다

Grafana Explore에서 사람이 읽을 때의 표준 쿼리:

```logql
{service_name=~"content-service|auth-service|chat-service"} | json
  | line_format "{{.level}} [{{.traceId}}] {{.logger_name}} — {{.message}}"
```

- 로컬 개발(`local`·`dev` 콘솔 실행)은 지금의 평문 패턴을 그대로 둔다.
  structured logging은 **수집 경로 프로파일에만** 건다.

### R7 — message 작성 규칙 (사람이 읽는 유일한 자유 텍스트)

- **한 문장으로 "무엇이 어떻게 됐다"**. 식별자는 문장 뒤에 `key=value`로:
  `재고 차감 실패 productId=123 requested=5 remaining=2`
- 레벨 기준:

| 레벨 | 기준 |
|---|---|
| ERROR | 이 요청/메시지가 실패로 끝났다. 스택 또는 원인 문자열 필수 |
| WARN | 성공했지만 정상 경로가 아니다 (재시도 성공, 폴백, 임계 근접) |
| INFO | 상태 전이 (기동, 컨슈머 시작/정지, 배치 시작/완료) |
| DEBUG | 개발용. 수집 경로에서는 꺼진다 |

- 같은 실패를 층마다 중복 로깅하지 않는다 — 잡아서 다시 던질 거면 로그는 최종
  처리 지점 한 곳에서만. (결함 #3의 앱 측 원인이기도 하다.)

### R8 — 세 서비스는 이 문서의 스니펫을 그대로 쓴다

- 설정 블록은 §4의 스니펫이 SoT이고, 레포별로 변형하지 않는다.
  변형이 필요해지면 **이 문서를 먼저 고치고** 세 레포에 같이 반영한다.

### R9 — 요청 자동 기록은 실패·저속만. 성공 요청은 트레이스가 SoT다

세 서비스 모두 요청 단위 자동 기록 필터를 두되, 남기는 것은:

| 조건 | 레벨 | 예 |
|---|---|---|
| 5xx | ERROR | `[HTTP] POST /api/comments 500 - 41ms` |
| 4xx | WARN | `[HTTP] GET /api/users/me 401 - 3ms` |
| 1초 초과 | WARN | `[HTTP-SLOW] GET /api/feeds 200 - 1840ms` |
| 그 외 성공 | **기록하지 않는다** | — |

- **성공 요청의 전량 기록은 트레이스(Tempo, 샘플링 1.0)가 SoT다.** 로그에 또 쓰면
  완전 중복이고, 실측으로 1시간 2,300줄 중 ERROR/WARN 8줄 — 부피의 대부분(~287배)이
  성공 INFO였다. traceId 전량 조회·스윕 집계가 이 부피를 그대로 진다.
- 현재 상태 (2026-07-31 레포 실측): **toy-content만** `RequestLoggingFilter`가 있고
  성공까지 INFO로 남긴다(축소 대상). toy-chat·auth(toy-user)는 요청 자동 기록이
  없다(추가 대상 — 401이 로그에 한 줄도 안 남는 관측 사각지대가 AU-2·AU-3에서 실증됐다).
- **적용 시 확인 사항**: content의 성공 INFO 제거는 기존 채점 앵커가 `[HTTP]` INFO 줄에
  의존하지 않는지 먼저 확인한다 (앵커 개정은 다음 회차부터라는 규율과 맞물린다).
- **재검토 조건**: 트레이스 샘플링을 1.0 미만으로 내리는 날, 성공 기록의 유일 채널이
  로그가 되므로 이 규칙을 다시 판단한다 (필터 레벨 한 줄 변경으로 전환 가능).

---

## 4. 표준 설정 스니펫

수집 경로(prod) 프로파일 공통 — toy-content · toy-chat (Boot 3.4+):

```yaml
spring:
  output:
    ansi:
      enabled: never

logging:
  structured:
    format:
      console: logstash
```

- 기존 `logging.pattern.level`(traceId MDC 주입)은 **평문 프로파일용으로 남긴다** —
  structured 포맷에서는 MDC가 자동으로 최상위 키가 되므로 pattern은 관여하지 않는다.
- toy-content의 파일 로그(`/logs/application.log`)는 수집 경로가 아니면 평문 유지,
  수집 대상이면 `logging.structured.format.file: logstash`로 동일 적용.

**auth(toy-user)는 선결 과제가 하나다** — 트레이싱은 이미 있고(2026-07-31 정정),
Boot 3.2.1이라 **내장 structured logging(R1)이 없다.** 3.4+ 업그레이드 권장.
업그레이드가 부담이면 대안은 `logstash-logback-encoder`(같은 logstash JSON을 만드는
원조 라이브러리)이지만, logback.xml 관리가 생기므로 R1 취지에서 차선이다.

---

## 5. rca-agent 쿼리가 어떻게 바뀌나 (B군 — 별도 회차)

이 절은 예측이다. 적용은 rca-agent 쪽 회차 대기열에서 별도로 다룬다.

| 쿼리 | 지금 (평문 우회) | 전환 후 |
|---|---|---|
| errorWarn | `\|~ "ERROR\|WARN\|Exception\|Caused by\|\.java:[0-9]+\)"` | `\| json \| level=~"ERROR\|WARN"` |
| traceId | `\|= "<traceId>"` (스택 미도달 → 정규식 보강) | `\| json \| traceId="<id>"` (스택은 `stack_trace` 필드로 같이 옴) |
| 스윕 집계 | 스택 패턴 제외 특례 | 특례 불필요 — 이벤트 단위 집계 |

**전환기 주의:** `| json`은 평문 줄에서 파싱 에러로 떨어진다. 조사 창이 배포 시점을
걸치면 평문·JSON이 혼재해 어느 쿼리로도 반쪽만 보인다 — **전환은 회차 경계에서만** 한다.

---

## 6. 적용·검증 절차

rca-agent 평가 규율(변경군 분리·baseline 고정)을 따른다. 이 변경은 **A군(앱 계측)** 이다.

1. **B군(조사 도구)과 같은 회차에 넣지 않는다.** 점수가 움직여도 원인을 못 가린다.
2. 배포 후, 조사를 돌리기 **전에** 신호 도달을 직접 확인한다:

| # | 확인 | 방법 | 통과 기준 |
|---|---|---|---|
| 1 | Loki 원문이 JSON 그대로인가 | Explore에서 파서 없이 원문 조회 | CRI 래퍼 이중 포장·ANSI 없음 |
| 2 | level 필드 생성 | 에러 1건 유발 후 `\| json \| level="ERROR"` | 해당 이벤트 1건 도달 |
| 3 | 스택 도달 (B-11 해소) | 예외 유발 후 `\| json \| traceId="<id>"` | `stack_trace`에 예외 전문 |
| 4 | 집계 단위 | `count_over_time` 전후 비교 | 발생 1건 = 집계 1건 |

3. **반증 조건:** #3이 실패하면(JSON인데도 스택이 traceId 쿼리에 안 옴) "줄 분리가
   원인"이라는 진단이 틀린 것이다 — 수집기(Alloy) 단을 다시 본다.
4. **토큰 영향은 미측정.** 예측: 스택 N줄 → 1이벤트로 줄 수는 줄고, 키 반복으로
   줄당 길이는 는다. 순효과는 전환 회차에서 동일 문항 재조사로 실측한다.

### 현재 상태 (2026-07-31)

| 항목 | 상태 |
|---|---|
| 규칙 제정 (이 문서) | 완료 |
| R1 JSON 구조화 | 미적용 (auth는 Boot 3.2.1이라 선결: 업그레이드) |
| R9 요청 자동 기록 (content 축소 · chat/toy-user 추가) | **코드 반영 완료 (2026-07-31, 커밋·배포 전).** 앵커 의존 확인됨 — chaos 앵커에 `[HTTP]` 의존 없음, 조사 리포트 인용은 전부 500 ERROR 줄이라 유지됨 |
| auth 로그 보강 (toy-user) | **코드 반영 완료 (2026-07-31)** — JWT 검증 실패 사유 로그(A-0 동일 코드) · JwtFilter USER_NOT_FOUND 사유 · ControllerAdvice `RestApiException` 무로그 해소 · `handleAllException` WARN→ERROR |
| **auth 레포 이원화** | **미해결** — `toy-user`(이번 반영)와 `toy-auth-user-region`(A-0 기반영) 중 **배포본 확정 필요.** 배포본이 region이면 R9·로그 보강을 그쪽에 포팅해야 한다 |
| 부수 수정: chat `logging.level` 키 오타 | 완료 — `com.example.chat`(존재하지 않는 패키지) → `com.example.toychat` |
| rca-agent 쿼리 전환 (§5) | 미착수 — 회차 대기열 등재 필요 |
| 검증 4종 (§6) | 미측정 |

R9 적용 시 INFO 공백 여부를 실사했다 — **추가 INFO는 불필요했다.** R7의 상태 전이는
이미 커버돼 있다: content 스케줄러 5종(시작/완료/실패), chat WebSocket
연결/해제/구독/해제(handler INFO), Kafka DLQ 재처리(수신/성공/실패). content에는
Kafka 리스너가 없다. 성공 HTTP의 전량 기록은 R9대로 Tempo가 담당한다.
