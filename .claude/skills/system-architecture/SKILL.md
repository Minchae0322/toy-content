---
name: system-architecture
description: 요거뜨 콘텐츠 서비스의 시스템 아키텍처·서비스 경계·인프라·배포 흐름·데이터스토어 사용 맥락을 참조할 때. 아키텍처/배포/서비스 간 연동/인프라/관측성 관련 질문에 답하거나, 서비스 경계를 넘는 변경(다른 레포 연동, Kafka 이벤트, 배포 방식)을 설계·검토할 때 사용한다.
---

# 시스템 아키텍처 — 요거뜨 콘텐츠 서비스

이 레포(content-service)가 전체 시스템 어디에 위치하고 무엇과 어떻게 연결되는지를 담는다. 서비스 경계를 넘는 작업(다른 레포 호출, 이벤트 발행, 배포/롤백) 전에 이 맥락을 먼저 확인한다.

## 전체 구성

AWS VPC 안 **K3s 클러스터**(EC2 3대: master 1 + worker 2) 위에서 세 개의 Spring Boot 서비스가 동작한다.

- **Frontend** — Vue.js 정적 빌드 → S3 + CloudFront
- **API 트래픽** — NGINX Ingress → 각 서비스로 라우팅
- **Backend** — `auth` / `content`(이 레포, **2 replica**) / `chat` 파드
- **Data** — MySQL(Amazon RDS), Redis, Kafka. MongoDB는 chat 전용.

시각 자료: `docs/assets/architecture-system.png`(시스템), `docs/assets/architecture-observability.png`(관측성).

## 서비스 경계

| 서비스 | 책임 | content(이 레포)와의 관계 |
|---|---|---|
| **content** (이 레포) | 배틀·피드·제품·보상 등 콘텐츠 도메인 + 알림 발행. 전체 트래픽의 약 70% | — |
| **auth-service** (`toy-auth-user-region`) | 회원·팔로우·JWT 발급 | JWT는 **공유 시크릿으로 이 서비스에서 직접 검증**한다(호출 없이). 사용자 정보는 API로 조회 후 **Redis 캐시**, 실패 시 **fallback 사용자**로 대체 — `external/user/` 참고 |
| **chat-service** | 실시간 채팅 | 한입만 판매글의 채팅방 수 집계에 연동 |

경계 원칙:
- 사용자 인증은 auth를 **호출하지 않고** JWT를 로컬 검증한다. 사용자 프로필 등 부가 정보만 `external/user` 클라이언트로 조회하고 캐시한다.
- 외부 사용자 조회는 항상 실패를 가정한다(fallback 사용자). 조회 실패가 요청 전체를 깨뜨리지 않게 한다.

## 데이터스토어 사용 원칙

- **MySQL(RDS)** — 도메인 영속화(JPA/Hibernate + QueryDSL 동적 쿼리).
- **Redis** — (1) 사용자/팔로잉 등 외부 조회 캐시, (2) 스케줄러 분산락(**ShedLock**) — content가 2 replica라 배치 중복 실행을 막는다.
- **Kafka** — 알림 이벤트 발행. **userId를 파티션 키**로 써서 사용자별 순서를 보장한다. 발행은 **트랜잭션 커밋 이후(AFTER_COMMIT)** 에만 — 취소된 행동의 유령 알림 방지.

## 배포 · 롤백 (GitOps)

- `main` 푸시 → GitHub Actions가 이미지 빌드 → **GHCR** 푸시 → 매니페스트 레포의 kustomize 이미지 태그 갱신 → GitOps로 배포.
- 롤백은 커밋 SHA를 지정하는 `.github/workflows/rollback.yml`.
- `/actuator/info`에 빌드 시간·버전이 노출됨(배포 식별용). actuator는 **관리 포트 8090**으로 분리해 외부 미노출.
- 앱: 포트 **8082**, context-path **`/api`**. graceful shutdown(최대 30s)으로 파드 종료 시 in-flight 요청 보호.

## 관측성

Alloy 수집기 하나로 3개 서비스의 메트릭·로그·트레이스를 모아 Grafana Cloud로 전송.

- **Metrics** — Micrometer → Prometheus. 응답시간 p50/p95/p99 히스토그램, Four Golden Signals 대시보드 + 이메일 알림.
- **Traces** — Tempo. `@Observed`로 서비스/스케줄러 메서드 span, JDBC 자동 계측으로 SQL span까지.
- **Logs** — 모든 로그에 traceId·userId를 심어 Loki ↔ Tempo 상호 이동.
- 관측성 작업은 `docs/observability/observability.md`에 누적 기록한다.

## 관련 레포

전체는 4개 레포로 나뉜다(콘텐츠 이 레포, 인증 `toy-auth-user-region`, chat, 매니페스트). 서비스 간 연동 작업 시 대상 레포와 포트/브랜치를 먼저 확인한다.
