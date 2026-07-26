# Chaos 실행 명령 — 복붙용

문항별 실행 순서를 그대로 붙여넣게 정리했다. 각 명령은 `chaos.sh`의 한 단계와 1:1이다.
배경·판정 기준·전제는 [RUNBOOK.md](RUNBOOK.md)를, 실행 위치(master/infra/아무 셸)는 RUNBOOK §2를 본다.

> ⚠️ **프로덕션 대상이다.** `chaos.env`의 `BASE`가 실서비스를 가리키고, 주입은 실제 컨테이너 중지·`replicas=0`·secret 변경이다.
> **한 번에 한 문항만**, 저트래픽 시간대에, `off`로 복귀 확인까지 마친 뒤 다음 문항으로 넘어간다.

## 0. 매번 먼저 (한 번)

```bash
cd docs/chaos/scripts
# chaos.env가 없으면: cp chaos.env.example chaos.env  → 값 채우기 (RUNBOOK §10 Step 0)
# GRAFANA_TOKEN 연결 확인:
source chaos.env
curl -s -u "$PROM_USER:$GRAFANA_TOKEN" -G "$PROM_URL/api/v1/query" --data-urlencode 'query=up' | jq '.status'
# → "success" 나오면 OK
```

## 단계 의미 (모든 문항 공통)

| 단계 | 하는 일 | 장애 |
|---|---|---|
| `baseline` | ① 정상 측정 + 게이트. **빈 값이면 exit 1로 주입 차단** | 없음 |
| `on` | ③ 주입 (docker stop / scale=0 / secret patch) | **여기서 발생** |
| `trigger` | ③ 부하 발생 (**CH-2, AU-1만**) | 부하 |
| `symptom` | ④ 장애 중 측정 (①과 같은 쿼리) | 측정만 |
| `off` | ⑤ 원복 + 복귀 확인 폴링 | 복구 |

- `on` 다음 **symptom 전에 ~1분 대기** (스크레이프·로그 전송 지연).
- 수동 단계 모드는 **자동 원복이 없다.** `on` 했으면 반드시 `off`로 닫는다.
- 통째로 돌리려면 `./chaos.sh <ID> run` (단계마다 Enter 확인, Ctrl-C 시 자동 원복 trap).

---

## AP-1 — 댓글 201자 (코드 결함 — 인프라 무접촉, 여기서 워밍업)

AP 계열의 `on`은 인프라 조작이 아니라 **경계값 실요청 1건**(즉발)이다. sleep은 로그 전송 대기용. 원복할 인프라가 없어 `off`는 정상 요청 복귀 확인만 한다. 전제: `chaos.env`에 `FEED_ID`.

```bash
cd docs/chaos/scripts
./chaos.sh AP-1 baseline    # 정상 댓글 200 + "Data too long" 0건
./chaos.sh AP-1 on          # 250자 댓글 전송 → 기대 500
sleep 60
./chaos.sh AP-1 symptom     # 정상 댓글은 여전히 200 / Loki Data too long / INSERT span error
./chaos.sh AP-1 off         # 원복 없음 — 정상 댓글 200 재확인 + 테스트 댓글 정리 메모
```

## AP-3 — 이모지 댓글 (조건부 — 200이면 불성립, 그 기록도 산출물)

```bash
./chaos.sh AP-3 baseline    # ASCII 댓글 200 + "Incorrect string value" 0건
./chaos.sh AP-3 on          # 이모지 댓글 — 200: utf8mb4 확인(불성립 종료) / 500: 증상 채록 진행
sleep 60
./chaos.sh AP-3 symptom
./chaos.sh AP-3 off
```

## AU-2 — auth 완전 다운 (인프라 문항 중 가장 단순, 인프라 계열은 여기서 시작)

```bash
cd docs/chaos/scripts
./chaos.sh AU-2 baseline
./chaos.sh AU-2 on          # auth deploy replicas=0
sleep 60
./chaos.sh AU-2 symptom     # 로그인 502 / content는 캐시 히트분만 정상
./chaos.sh AU-2 off         # replicas=1 + 로그인 200 복귀까지 폴링
```

## AU-1 — auth CPU 기아 (trigger 있음)

```bash
./chaos.sh AU-1 baseline
./chaos.sh AU-1 on          # cpu limit 50m patch
./chaos.sh AU-1 trigger     # 로그인 20회
# fallback은 user 캐시 TTL 경과 후 관측 — TTL만큼 더 기다렸다가:
./chaos.sh AU-1 symptom     # 로그인 P99 급등 / client span 3s 잘림 / 작성자 익명
./chaos.sh AU-1 off         # cpu 원복(AUTH_CPU_NORMAL)
```

## AU-4 — auth 다운 + user 캐시 만료 (fallback 실검증, AU-2의 캐시 만료판)

AU-2와 주입은 같지만 **캐시 TTL(10분) 경과까지 유지**해야 content가 auth 직행 → fallback 경로가 열린다. 10분 전엔 캐시 히트라 AU-2와 구별 안 됨.

```bash
./chaos.sh AU-4 baseline    # 로그인 200 / T2(?size=10) 작성자 실명
./chaos.sh AU-4 on          # auth deploy replicas=0 (+ 캐시 TTL 10분 안내)
# 10분+ 대기 — 그동안 T2를 주기적으로 쳐 실명→익명 전환 관측 권장
./chaos.sh AU-4 symptom     # 로그인 503 / T2 작성자 익명 "사용자N"이면 fallback 정상, 500이면 붕괴
./chaos.sh AU-4 off         # replicas=1 + 로그인 200 복귀까지 폴링
```

## CH-1 — Mongo 다운 → 재시도 → DLQ

```bash
./chaos.sh CH-1 baseline
./chaos.sh CH-1 on          # infra 노드에서 docker stop $MONGO_CT
sleep 60
./chaos.sh CH-1 symptom     # 댓글 200 + 알림 미도착 / DLQ·Retry 로그 / 재시도 span
./chaos.sh CH-1 off         # docker start + Running 폴링
```

## IN-2 — Kafka 다운 (조용한 유실)

```bash
./chaos.sh IN-2 baseline
./chaos.sh IN-2 on          # docker stop $KAFKA_CT
sleep 60
./chaos.sh IN-2 symptom     # 댓글 200 + 알림 영구 유실 / producer span error 태그 유무
./chaos.sh IN-2 off         # docker start (주입 중 유실분은 복구 불가 — answer.md 기록)
```

## IN-1 — Redis 다운 (다중 서비스, 최고 난도)

```bash
./chaos.sh IN-1 baseline
./chaos.sh IN-1 on          # docker stop $REDIS_CT
# 스케줄러 증상(핫스코어 갱신 정체)은 주기 도래까지 수 분 대기
./chaos.sh IN-1 symptom     # content 캐시미스 직행 급증 / chat 프레즌스 이상 / 스케줄러 skip
./chaos.sh IN-1 off         # docker start + Running 폴링
```

## IN-3 — 커넥션 풀 고갈 (on이 k6 부하라 블로킹)

`on`이 k6 부하를 **블로킹으로** 실행한다(부하 종료 = 해제). 부하가 도는 동안 측정하려면 **터미널 2개**를 쓴다.

```bash
# 터미널 A
cd docs/chaos/scripts
./chaos.sh IN-3 baseline
./chaos.sh IN-3 on          # k6 부하 시작 — 끝날 때까지 이 터미널 블로킹

# 터미널 B (부하 도는 동안)
cd docs/chaos/scripts
./chaos.sh IN-3 symptom     # hikaricp pending >0 / P0 알람 / JDBC span 앞 공백

# 터미널 A (k6 종료 후)
./chaos.sh IN-3 off         # pending 0 복귀까지 폴링
```

## AP-2 — 첨부 대용량 업로드 (계층 판별 — 대역폭 부하, 저트래픽·1회만)

```bash
./chaos.sh AP-2 baseline    # 1KB 업로드 200 — 500이면 잠복 버그(경로 구분자) 실증, 기록 후 중단
AP2_MB=2 ./chaos.sh AP-2 on # 2MB 업로드 — 통과(200)하면 AP2_MB=1100으로 재시도
sleep 60
./chaos.sh AP-2 symptom     # 413+HTML+앱로그 없음=ingress / 500+JSON=multipart 미매핑 / 200=무제한 실측
./chaos.sh AP-2 off         # 임시 파일 삭제 — 서버에 올라간 성공분은 수동 정리
```

## AU-3 — JWT 시크릿 드리프트 (전 사용자 영향 — 가장 마지막·가장 짧게)

```bash
./chaos.sh AU-3 baseline
./chaos.sh AU-3 on          # secret 백업 → JWT_SECRET 변경 → content rollout restart
sleep 60
./chaos.sh AU-3 symptom     # 로그인은 성공, content 인증 API 전부 401
./chaos.sh AU-3 off         # 백업 apply + restart + T4 200 복귀 폴링 (백업 파일 자동 삭제)
```

## CH-2 — 컨슈머 정지 → lag 누적

전제(lag 메트릭)는 kafka-exporter의 `kafka_consumergroup_lag`로 충족 확인됨(2026-07-26, RUNBOOK §6 CH-2). 게이트가 여전히 지켜준다 — 빈 값이면 `baseline`이 exit 1.

```bash
./chaos.sh CH-2 baseline    # notif_lag(exporter)·ws_active_users 존재 확인 — 빈 값이면 중단 = 주입 금지
./chaos.sh CH-2 on          # chat deploy replicas=0
./chaos.sh CH-2 trigger     # 댓글 30건 × 10초 간격 ≈ 5분 (블로킹)
./chaos.sh CH-2 symptom     # lag 누적 / active_users 0 / content 트레이스는 무결
./chaos.sh CH-2 off         # replicas=1 + rollout + 밀린 알림 일괄 도착 확인
```

---

## 권장 순서 (RUNBOOK §7.1)

hop이 짧고 주입이 단순한 것부터, 전 사용자 영향은 마지막. AP 계열(인프라 무접촉)은 워밍업으로 맨 앞 — 단 AP-2는 대역폭 부하가 있어 부하 문항 옆.

```
AP-1 → AP-3 → AU-2 → AU-1 → CH-1 → IN-2 → IN-1 → IN-3 → AP-2 → AU-3    (CH-2는 §10 lag 작업 완료 후)
```

각 문항 완료 후 채점 결과는 `scenarios/<ID>/answer.md`에, evidence는 `scenarios/<ID>/evidence/`에 자동 저장된다.

**반복이 측정의 조건이다(RUNBOOK §8.1):** 전 문항은 **2~3회, 다른 날**에 반복 실행·독립 채점하고 평균±편차로 기록한다 — 1회 점수는 인용하지 않는다. 채점은 answer.md에 미리 박제된 **채점 앵커**(§8.2) 기준으로만 한다.
