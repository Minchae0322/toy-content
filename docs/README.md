# docs — 문서 인덱스

요거뜨 content-service의 설계·운영·관측성 문서 모음. 주제별 디렉토리로 정리돼 있다.

## 디렉토리 지도

| 경로 | 내용 |
|---|---|
| [`domain/`](domain/) | 도메인별 설계 문서 |
| [`observability/`](observability/) | 관측성 작업 로그 · 트레이싱 커버리지 검증 |
| [`chaos/`](chaos/README.md) | 장애 주입 & RCA 품질 검증 런북 |
| [`blog/`](blog/) | 기술 회고 글 |
| [`migrations/`](migrations/) | DB 마이그레이션 SQL |
| [`assets/`](assets/) | 이미지 (아키텍처 다이어그램·화면·트레이스 캡처) |

## domain — 도메인 설계

| 문서 | 내용 |
|---|---|
| [reward-domain.md](domain/reward-domain.md) | EXP·레벨·뱃지·미션·스트릭 보상 체계 |
| [badge-design.md](domain/badge-design.md) | 뱃지 설계 |
| [battle-vote-guest.md](domain/battle-vote-guest.md) | 배틀 게스트(비로그인) 투표 |
| [swipe-battle-frontend.md](domain/swipe-battle-frontend.md) | 스와이프 배틀 프론트 연동 |
| [feedback-api.md](domain/feedback-api.md) | 피드백 API |
| [report-api.md](domain/report-api.md) | 신고 API |

## observability — 관측성

| 문서 | 내용 |
|---|---|
| [observability.md](observability/observability.md) | 관측성 구축 누적 작업 로그 (**관측성 작업은 여기 기록**) |
| [tracing-coverage.md](observability/tracing-coverage.md) | 트레이싱 경계 전수 열거 + 검증 상태 |
| [tracing-coverage.postman_collection.json](observability/tracing-coverage.postman_collection.json) | 위 검증 트래픽을 흘리는 Postman 컬렉션 |

## chaos — 장애 주입 & RCA

[`chaos/README.md`](chaos/README.md)부터. 복붙 실행은 [`chaos/COMMANDS.md`](chaos/COMMANDS.md), 상세 런북은 [`chaos/RUNBOOK.md`](chaos/RUNBOOK.md).

## assets — 이미지

| 파일 | 쓰임 |
|---|---|
| `architecture-system.png` | 시스템 아키텍처 (루트 README) |
| `architecture-observability.png` | 관측성 아키텍처 (루트 README) |
| `screen-web.png` / `screen-mobile.png` | 웹·모바일 화면 (루트 README) |
| `trace-secured-request.png` | 인증 요청 트레이스 워터폴 (tracing-coverage.md) |
