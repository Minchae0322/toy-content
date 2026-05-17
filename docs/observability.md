# Observability — 메트릭 / 트레이싱 / 로깅

Grafana Cloud Free(메트릭 10k 시리즈, 로그·트레이스 각 50GB/월, retention 14일) 위에 K3s + Alloy 1개로 통합 수집하는 구조. Spring Boot 측 인프라(Actuator + Micrometer + Brave/Zipkin)를 표준화해두고, 트래픽 성장 시 자체 호스팅 LGTM(VictoriaMetrics + Hybrid Pull/Push)으로 마이그레이션할 수 있도록 앱 코드가 모니터링 백엔드에 결합되지 않게 설계한다.

이 문서는 **앱 레벨에서 일어난 결정과 변경**을 누적해서 기록한다. Helm/Alloy/대시보드 설치는 별도 진행.

---

## 2026-05-17 — Spring Boot Actuator/Micrometer 표준화

### 무엇을 했나

| 영역 | 변경 |
|---|---|
| `application.yml` (base) | 모든 프로파일 공통 — `spring.application.name=content-service`, `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=30s`, `management.*` 블록(8090 포트 분리, prometheus 노출, SLO 버킷, B3 propagation), `logging.pattern.level`로 traceId/spanId/userId MDC 출력 |
| `MetricsConfig.java` (신규) | **카디널리티 방어** — `http.server.requests`의 `uri` 태그 100개 상한 + `/actuator/*` scrape 노이즈 제외 |
| `JwtFilter.java` | JWT 검증 직후 `MDC.put("userId", ...)`, `finally`에서 `remove`. trace ↔ 로그 ↔ 메트릭 연결의 마지막 1칸 |
| `build.gradle` | `springBoot { buildInfo() }` — `/actuator/info`에 빌드 시간/버전 노출 (배포 식별용) |

이전 단계(`f42a37f`, `5812d55`, `7c5319a` — 2026-05-04~07)에서 prod·dev 프로파일에는 이미 같은 management 블록이 들어가 있었지만, base에 없어서 신규 프로파일이나 local 환경에선 적용되지 않았다. 이번에 base로 끌어올려 어디서 띄우든 동일한 메트릭/트레이스/로깅 패턴이 자동 적용된다.

### 핵심 의사결정

- **관리 포트 8090 분리** — `/actuator/*`를 서비스 포트(8082)와 같이 노출하면 NetworkPolicy로만 차단해야 하고 누락 위험이 있다. 포트 자체를 분리해서 외부 LB는 8082만 알게 한다.
- **`jvm.classes`, `jvm.buffer`, `jdbc`, `tomcat.cache`, `tomcat.servlet` 비활성** — 다 켜면 10k 시리즈 한도가 하루도 못 간다. 운영 가치 대비 카디널리티가 너무 높은 메트릭들.
- **MeterFilter 우선순위 최상** — yml `metrics.enable.*`는 미터 자체 on/off만 가능하고, 태그 카디널리티는 코드 레벨 MeterFilter로만 잡힌다. Grafana Free의 한도 보호는 사실상 이 한 빈에 달려있다.
- **B3 propagation** — auth/chat 서비스가 같은 B3를 쓰기로 했으므로 MSA trace 전파 호환성을 위해 W3C 대신 B3 고정.
- **`TRACING_SAMPLING_RATE` 기본 1.0** — 사이드 프로젝트 규모라 풀 샘플링이 비용 부담 없음. 트래픽 증가 시 환경변수로 0.1 등으로 낮춘다.

### 검증 방법

```bash
# Pod 띄운 후
curl http://localhost:8090/actuator/health
curl http://localhost:8090/actuator/prometheus | grep -E "http_server|jvm_memory|hikari"
```

로그에 `[traceId=xxx,spanId=yyy,userId=NONE]` 패턴이 찍히고, `/actuator/prometheus` 응답에 `http_server_requests_seconds_*`, `jvm_memory_used_bytes`, `hikaricp_connections_active`가 보이면 Spring 측 작업은 완료.

### 다음 단계

- [ ] K3s worker1에 Grafana Cloud Helm chart(`k8s-monitoring`)로 Alloy DaemonSet + Deployment 설치
- [ ] infra 노드 docker-compose에 `kafka-exporter`/`redis-exporter`/`mongodb-exporter` 추가, VPC 사설 IP로 worker1 Alloy가 scrape
- [ ] 공식 대시보드 import: 17175 (Spring Boot 3 Stats), 11378 (JVM Micrometer), 6417 (K8s Cluster), 763 (Redis), 7589 (Kafka Exporter)
- [ ] Critical 알림 4개: 5xx 비율 / OOMKilled / Kafka consumer lag / p99 SLO 위반
- [ ] 비즈니스 메트릭 추가: `yogurtte.feed.created`, `yogurtte.battle.vote` 등
- [ ] k6 부하 테스트로 N+1 발견 → `@EntityGraph` 적용 → p99 개선 사례 만들기
- [ ] 24h Soak Test로 WebSocket(chat) 누수 검증

---

## 향후 기록 형식

새 변경이 생기면 위에 `## YYYY-MM-DD — 제목` 섹션을 추가한다. 각 섹션은 다음 4개를 포함:

- **무엇을 했나** — 파일 단위로 표
- **핵심 의사결정** — 왜 그렇게 했나 (대안 대비 트레이드오프)
- **검증 방법** — 어떻게 확인했나
- **다음 단계** — 후속 작업 체크박스

git log에 이미 있는 단순 코드 변경 내역보다, "왜 그런 결정을 했는지"와 "면접에서 답변할 컨텍스트"에 무게를 둔다.
