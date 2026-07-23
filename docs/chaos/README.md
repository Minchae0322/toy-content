# docs/chaos — 장애 주입 & RCA 품질 검증

AI 기반 RCA(trace·Loki·metrics로 원인 분석)의 품질을 재려면 **원인을 내가 아는 실제 장애**가 필요하다.
각 문항 = 실제 인프라 주입 + 정답지 + 근거 시그널 + 복구 절차로 구성된 테스트케이스이며,
**정상 측정 → 기록 → 주입 → 증상 대조 → 원복 → 블라인드 채점** 사이클로 실행한다.

## 파일 지도

| 파일 | 용도 |
|---|---|
| **[COMMANDS.md](COMMANDS.md)** | 복붙용 실행 명령 — 문항별 `baseline→on→(trigger)→symptom→off` 시퀀스. **여기서 시작.** |
| **[RUNBOOK.md](RUNBOOK.md)** | 마스터 런북 — 설계 원칙·실행 위치(§2)·사이클(§3.3)·문항별 상세(§6)·안전(§7)·채점(§8)·전제(§10) |
| `scripts/chaos.sh` | 사이클 실행기. `./chaos.sh <ID> baseline\|on\|trigger\|symptom\|off\|run` |
| `scripts/chaos.env.example` | 설정 템플릿. 복사해 `chaos.env`로 채운다 |
| `scripts/chaos.env` | 실제 값 (**gitignore** — 시크릿·사설 IP·Grafana 토큰) |
| `scenarios/<ID>/answer.md` | 문항별 정답지 + RCA 채점 결과 |
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
| IN-1 | Redis 다운 → 3개 서비스 동시 이상 | `docker stop $REDIS_CT` | 다중 | 스케줄러 @Observed |
| IN-2 | Kafka 다운 → 알림 조용히 유실 | `docker stop $KAFKA_CT` | 2 | 없음 |
| IN-3 | 커넥션 풀 고갈 → 전면 지연 | k6 부하 | 2 | Alert P0 룰 |

권장 실행 순서: `AU-2 → AU-1 → CH-1 → IN-2 → IN-1 → IN-3 → AU-3` (CH-2 최후).

## 결과 매트릭스 (채록하며 채운다)

| 문항 | 주입 | 사용자 증상 | 근거 시그널 | RCA 점수 | 발견된 계측 구멍 → 보강 커밋 |
|---|---|---|---|---|---|
| | | | | | |
