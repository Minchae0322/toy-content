# docs/chaos — 장애 주입 & RCA 품질 검증

AI 기반 RCA(trace·Loki·metrics로 원인 분석)의 품질을 재려면 **원인을 내가 아는 실제 장애**가 필요하다.
각 문항 = 실제 인프라 주입 + 정답지 + 근거 시그널 + 복구 절차로 구성된 테스트케이스이며,
**정상 측정 → 기록 → 주입 → 증상 대조 → 원복 → 블라인드 채점** 사이클로 실행한다.

## 파일 지도

| 파일 | 용도 |
|---|---|
| **[RESULTS.md](RESULTS.md)** | **문항별 실행 결과 종합** — 실측·발견 버그·수정 검증·AI RCA 평가·포트폴리오 포인트. 결과가 궁금하면 여기부터. |
| **[COMMANDS.md](COMMANDS.md)** | 복붙용 실행 명령 — 문항별 `baseline→on→(trigger)→symptom→off` 시퀀스. **여기서 시작.** |
| **[RUNBOOK.md](RUNBOOK.md)** | 마스터 런북 — 설계 원칙·실행 위치(§2)·사이클(§3.3)·문항별 상세(§6)·안전(§7)·채점(§8)·전제(§10) |
| `scripts/chaos.sh` | 사이클 실행기. `./chaos.sh <ID> baseline\|on\|trigger\|symptom\|off\|run` |
| `scripts/chaos.env.example` | 설정 템플릿. 복사해 `chaos.env`로 채운다 |
| `scripts/chaos.env` | 실제 값 (**gitignore** — 시크릿·사설 IP·Grafana 토큰) |
| **[anchors-v2.md](anchors-v2.md)** | **v2 앵커 (회차 2~ · 자연어 입력) — 전 문항 SoT.** 질문 문안 박제 · 탐색 채널 감사 · 문항별 만점 요건. 2026-07-28 rca-agent `docs/scoring/`에서 이관(정답 요건은 피험자 레포 밖에 둔다) |
| `scenarios/<ID>/answer.md` | 문항별 정답지 + v0 앵커 + RCA 채점 결과 |
| `scenarios/<ID>/evidence/` | `chaos.sh`가 자동 저장하는 baseline/symptom 채록 |

## 빠른 시작

```bash
cd docs/chaos/scripts
cp chaos.env.example chaos.env      # 이미 있으면 생략 — 값은 RUNBOOK §10 Step 0
./chaos.sh AU-2 baseline            # 정상 측정 (장애 없음, 게이트 통과 확인)
./chaos.sh AU-2 on                  # 주입
sleep 60
./chaos.sh AU-2 symptom             # 증상 채록
./chaos.sh AU-2 off                 # 원복 + 복귀 확인
```

전체 문항 명령은 [COMMANDS.md](COMMANDS.md). **한 번에 한 문항만, 프로덕션 대상.**

## 문항 카탈로그

| ID | 시나리오 | 주입 | hop | 전제(§10) |
|---|---|---|---|---|
| CH-1 | Mongo 다운 → 컨슈머 재시도 → DLQ | `docker stop $MONGO_CT` | 2 | chat Step 0 |
| CH-2 | chat 다운 → lag 누적 → 복구 후 알림 몰림 | `scale --replicas=0` | 2 | **lag 메트릭 (미구현 시 보류)** |
| AU-1 | auth CPU 기아 → 지연 → content fallback | cpu limit 50m | 2~3 | (권장) auth JDBC 계측 |
| AU-2 | auth 완전 다운 → 로그인 502, content 익명 | `scale --replicas=0` | 2 | 없음 |
| AU-3 | JWT 시크릿 드리프트 → 인증 API 401 | secret 변경 + restart | 1 | 없음 |
| AU-4 | auth 다운 + user 캐시 만료 → fallback(익명) 검증 | `scale 0` + 10분 유지 | 2~3 | user 캐시 TTL 10분 |
| IN-1 | Redis 다운 → 3개 서비스 동시 이상 | `docker stop $REDIS_CT` | 다중 | 스케줄러 @Observed |
| IN-2 | Kafka 다운 → 알림 조용히 유실 | `docker stop $KAFKA_CT` | 2 | 없음 |
| IN-3 | 커넥션 풀 고갈 → 전면 지연 | k6 부하 | 2 | Alert P0 룰 |
| AP-1 | 댓글 201자 — 검증 구멍 → varchar(200) 위반 → 500 | 경계값 실요청 | 1 | FEED_ID |
| AP-2 | 대용량 업로드 — 실패 계층 판별(ingress/앱) | 대용량 실요청 | 1~2 | 없음 |
| AP-3 | 이모지 댓글 — charset 불일치(조건부) | 이모지 실요청 | 1 | FEED_ID |

권장 실행 순서: `AP-1 → AP-3 → AU-2 → AU-1 → CH-1 → IN-2 → IN-1 → IN-3 → AP-2 → AU-3` (CH-2 최후).
AP 계열은 인프라 무접촉(주입 = 경계값 실요청 1건, 원복 없음) — 코드에 잠복한 결함을 실요청으로 발화시키는 문항이다(RUNBOOK §6 AP 공통).

## 문항 설계 의도 — 무엇을 확인하려 했나

문항은 "장애를 만들 수 있다"의 나열이 아니라, **"이 시그널을 못 읽으면 이 장애를 못 찾는다"를 하나씩 증명하도록** 설계했다. 그래서 문항마다 노리는 변별 포인트와 빠지라고 파둔 함정이 있고, 겉보기 증상이 같은 문항을 일부러 쌍으로 뒀다.

| 문항 | 만드는 장애 | 사용자 증상 | 확인하려는 것 | 함정 · 대비쌍 |
|---|---|---|---|---|
| AU-2 | 인증 서비스 전면 다운 | 로그인 불가(502), 피드 작성자 일부 익명 | 의존 서비스 전멸 시 fallback 설계(캐시 히트 정상 + 미스 익명)가 실제로 동작하는가 | AU-1과 쌍 — 트레이스가 "즉시 실패"인가 "3s 잘림"인가 |
| AU-1 | 인증 서비스 CPU 기아(느려짐) | 로그인 느림, 작성자 익명 — **에러는 없음** | **에러율이 안 오르는 장애**를 잡는가 — 5xx만 보는 RCA는 여기서 실패한다 | 함정: 에러율 무변화. 성능·데이터 품질만 저하 |
| AU-3 | JWT 시크릿 불일치(config drift) | 로그인은 되는데 모든 기능이 401 | 코드·인프라 무고장 장애를 잡는가 — 401은 4xx라 **trace에 error가 안 남는다** | trace로 못 찾는 문항 — 메트릭+로그로만 도달 |
| AU-4 | auth 전면 다운 + user 캐시 만료 | 피드 작성자가 익명 "사용자N"으로 저하 | fallback 경로가 실제로 버티는가(익명 저하) vs 무너지는가(500) — 캐시가 가려주던 의존을 드러냄 | AU-2와 쌍 — AU-2는 캐시 히트로 무영향, AU-4는 캐시 만료로 fallback 실검증 |
| CH-1 | 알림 저장소(Mongo) 다운 | 댓글은 되는데 알림만 안 옴 | 비동기 소비자의 실패 격리 — 재시도→DLQ가 설계대로 작동하고 **한 트레이스로 남는가** + 유실 없음 판단 | 함정: 댓글 API는 200 — "200=정상" 오판 |
| CH-2 | 알림 소비자(chat) 전멸 | 알림 안 옴 + 채팅 두절 → 복구 시 몰아서 도착 | 발행측이 완전 무결할 때, 에러가 아니라 **메트릭(lag 누적)만으로** 소비 정지를 지목하는가 | IN-2와 쌍 — 같은 "알림 안 와요"인데 지연 후 전부 도착(유실 없음) |
| IN-2 | 메시지 브로커(Kafka) 다운 | 멀쩡해 보이는데 알림만 영구 유실 | **조용한 유실** — API 전부 200일 때 baseline과 대조해 "사라진 span"을 알아보는가. span조차 안 남으면 그게 계측 구멍 발견 | CH-2와 쌍 — 이쪽은 유실(복구 불가). 함정: 전 API 200 |
| IN-1 | 공유 인프라(Redis) 다운 | 서비스 3개가 **서로 다르게** 아픔 | 상이한 증상(캐시 직행 급증·스케줄러 skip·프레즌스 이상)을 **단일 근원으로 수렴**하는가 — 최고 난도 | 함정: 개별 장애 3건으로 쪼개면 오귀인. 2차 지연 증상(핫스코어 정체)까지 |
| IN-3 | 커넥션 풀 고갈(부하) | 무관한 API까지 전면 지연 | "DB가 느림"과 "커넥션을 못 얻음"을 구별하는가 — span이 **없는** 공백 구간을 읽는 능력 + P0 알람 실발화 검증 | 함정: DB 서버 탓 오귀인 |
| AP-1 | 앱 검증 구멍 (댓글 201자 → 500) | 긴 댓글만 등록 실패, 나머지 전부 정상 | 인프라가 전부 정상일 때 "왜 **이 요청만** 실패하나"로 수렴하는가 — rate 알람이 못 잡는 단발 오류 | 함정: SQL 예외 보고 DB 탓 — DB는 제약을 지켰다 |
| AP-2 | 업로드 한도·검증 부재 | 첨부 올리면 글 작성 실패 | 실패 **계층**(ingress / 앱)을 가르는가 — 앱 시그널의 **부재** 자체를 근거로 쓰는 능력 | 413+HTML(앱 도달 전) vs 500+JSON(앱) |
| AP-3 | DB charset 불일치 (이모지 → 500) | 이모지 댓글만 실패 | 같은 API·같은 500을 **로그 지문**으로 가르는가 (길이 vs 인코딩) | AP-1과 쌍 — Data too long vs Incorrect string value |

## 결과 매트릭스 (채록하며 채운다)

문항당 2~3회 반복 실행 후 평균±편차로 기록한다(RUNBOOK §8.1) — 1회 점수는 인용하지 않는다.

| 문항 | 주입 | 사용자 증상 | 근거 시그널 | RCA 점수 (평균±편차) | 발견된 계측 구멍 → 보강 커밋 |
|---|---|---|---|---|---|
| CH-1 | Mongo `docker stop` ×4회 (최장 4분 59초) | 댓글은 200, 알림만 지연(24.7s~3분 36초) — 유실 0 | 재시도 30.0s×4 트레이스 → DLQ 로그 → `mongodb_up=0` | 정식 채점 전 (블라인드 조사 2회는 [RESULTS.md](RESULTS.md) AE-01·AE-02) | 예외 삼킴 → `5eecb0a` / Mongo 트레이싱 → `cdca2a5` / 잔여는 RESULTS 표 |
| IN-2 | Kafka `docker stop` 1회 (5분 17초) | 없음 — API 전부 200, 알림 1건 조용히 영구 유실 | `알림 발행 실패` ERROR(+traceId) → 60,060ms error span → `kafka_brokers` 부재. chat은 침묵 | 정식 채점 전 (블라인드 조사 1회는 [RESULTS.md](RESULTS.md) AE-03) | chat dev 프로필·absent 알람·소비자 침묵 — 미수정 ([RESULTS.md](RESULTS.md) IN-2 표) |
