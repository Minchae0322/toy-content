# CLAUDE.md

요거뜨(Yogurtte)의 **콘텐츠 서비스(content-service)** 레포입니다. 전체 백엔드(auth / content / chat) 중 배틀·피드·제품·보상 등 핵심 도메인을 담당하며 트래픽의 약 70%를 처리합니다. 이 문서는 Claude Code가 이 레포에서 작업할 때 매번 필요한 맥락을 담습니다.

## 기술 스택

- **Java 17**, **Spring Boot 3.4.1**, Gradle
- **MySQL**(RDS) + JPA/Hibernate, **QueryDSL** (동적 쿼리)
- **Redis** (캐시 + ShedLock 분산락), **Kafka** (이벤트/알림)
- **Spring Security** + **JWT**(jjwt) — 무상태(stateless) 인증
- **관측성**: Micrometer Tracing(Brave/Zipkin→Tempo), Prometheus, `@Observed` AOP, JDBC 자동 계측
- **Springdoc OpenAPI**(Swagger), Hibernate Envers(감사)
- 배포: K3s on EC2, GitHub Actions → GHCR → GitOps(kustomize)

## 빌드 · 테스트 명령

```bash
./gradlew compileJava        # 컴파일만 (빠른 검증)
./gradlew test               # 전체 테스트 (JUnit5)
./gradlew build              # 빌드 + 테스트 (bootJar → build/libs/app.jar)
./gradlew compileTestJava    # 테스트 코드 컴파일 확인
```

- 코드 수정 후에는 최소 `./gradlew compileJava`, 로직 변경이면 `./gradlew test`로 검증합니다.
- QueryDSL Q타입은 `annotationProcessor`가 생성하므로, 리포지토리 커스텀 구현을 바꾸면 재컴파일이 필요합니다.

## 프로필 · 포트

- 프로필: `dev`(기본), `prod`, `local`. `SPRING_PROFILES_ACTIVE`로 제어.
- 앱 포트 **8082**, context-path **`/api`** (dev/prod). 관리 포트 **8090**(actuator/prometheus, context-path 없음).
- 시크릿·DB·JWT 비밀키는 모두 환경변수(`${...}`)로 주입. **설정 파일에 실제 시크릿을 하드코딩하지 말 것** (`application-local.yml`에 과거 하드코딩된 값이 남아 있으니 새로 추가 금지).

## 패키지 구조

`com.example.toycontent`
- `app/<domain>/` — 도메인별 `controller` / `service` / `domain` / `repository`(+`querydsl`). 도메인: `battle`, `feed`, `product`, `carrier`, `oneMouth`(한입만), `reward`, `feedback`, `hashtag`, `category`, `post`, `dashboard`, `notification`, `file`.
- `app/auth/` — `JwtFilter`(실제 인증), `JwtParser`, `CustomUserDetails`.
- `app/config/` — `SecurityConfig`, `KafkaConfig`, `RedisConfig`, `TracingConfig`, 스케줄러/JPA 설정 등.
- `app/common/` — 공통 애노테이션(`@CurrentUserId`, `@CurrentUserIsAdmin`, `@CheckAdmin`), 리졸버, 예외.
- `external/user/` — 인증(user) 서비스 호출 클라이언트 + 캐시.

## 코딩 컨벤션 (DTO · 설계 패턴)

- **DTO 분리**: 요청/응답 DTO는 도메인별 `XxxRequest` / `XxxResponse` 클래스 안에 **오퍼레이션별 static 이너 클래스**로 정의한다. 예: `RewardRequest.CreateBadge`, `RewardResponse.BadgeInfo`, `ProductResponse.ProductList`. request와 response를 한 클래스에 섞지 않는다. 이너 DTO는 `record`(또는 불변) 권장.
- **엔티티 비노출**: JPA 엔티티를 컨트롤러 응답이나 요청 바디로 직접 노출하지 않는다. 항상 위 DTO로 변환한다.
- **팩토리 + 전략 패턴**: 타입에 따라 동작이 갈리고 **확장이 예상되는 영역**(예: 배틀 투표 방식 — 1인1표/1인3표/스와이프, 한입만 판매 유형 — 일반/공동구매/대리구매)은 `if/switch` 분기를 흩뿌리지 말고 **전략 인터페이스 + 팩토리**로 구현한다. 새 타입 추가가 전략 클래스 하나 추가로 끝나도록(OCP) 설계한다.
- **Clean Code · DDD**: 비즈니스 규칙은 서비스에 흩지 말고 **엔티티/도메인 객체 안**에 둔다(리치 도메인 모델). 애그리거트 경계를 존중하고, 다른 애그리거트는 ID로 참조한다. 의미 있는 이름, 작은 메서드, 부수효과 최소화를 지킨다. 서비스 계층은 오케스트레이션(트랜잭션·조합) 위주로 얇게 유지한다.

## 인증 · 인가 규칙 (중요)

- 인증은 `app/auth/filter/JwtFilter`(@Component 서블릿 필터)가 담당. `SecurityConfig`의 `JwtAuthenticationFilter`는 no-op이니 여기에 로직을 넣지 말 것.
- **메서드 보안이 켜져 있음**(`@EnableMethodSecurity`). 관리자 전용 엔드포인트에는 `@CheckAdmin`을 붙인다.
- JWT 권한 문자열은 **`ROLE_` 접두사 없이 `"ADMIN"`**. 그래서 권한 체크는 `hasRole` 대신 **`hasAuthority('ADMIN')`**(`@CheckAdmin` 내부)와 `@CurrentUserIsAdmin` 리졸버가 모두 `"ADMIN"`으로 일치시킨다. 새 권한 로직도 이 컨벤션을 따를 것.
- 컨트롤러에서 현재 사용자 정보는 파라미터 애노테이션으로 주입: `@CurrentUserId Long`, `@CurrentUserName String`, `@CurrentUserIsAdmin boolean`.
- 인증 없이 접근 가능한 경로는 `JwtFilter`의 `isOptionalAuthPath`(일부 GET)와 `shouldNotFilter`(login/oauth2/actuator/swagger)로 관리. 공개 엔드포인트를 추가하면 여기도 갱신해야 한다.
- 프로덕션에서 Swagger는 기본 비활성(`SWAGGER_ENABLED`).

## 도메인 컨벤션

- **알림·이벤트는 트랜잭션 커밋 이후 발행**한다(`AFTER_COMMIT`). 취소된 행동에 대한 유령 알림을 막기 위함 — 서비스 내 직접 발행 금지, 이벤트로 처리.
- **랭킹/핫스코어**는 스케줄러가 주기 집계(시간 가중치 모델). ShedLock으로 다중 replica 중복 실행 방지.
- 목록/피드 조회는 N+1을 경계하고 QueryDSL fetch join 또는 배치로 해소한다.
- 게스트(비로그인) 참여가 가능한 배틀 투표 경로가 있으니, 사용자 식별은 로그인/게스트 양쪽을 고려한다.

## 관측성 작업 규칙

- 관측성(observability) 관련 작업은 **`docs/observability/observability.md`에 누적 기록**한다.
- 서비스/스케줄러 메서드 span은 `@Observed`, SQL span은 JDBC 자동 계측으로 생성된다. 예외를 삼키는 지점은 span에 error 태그를 남긴다.

## 커밋 · 배포

- `main` 푸시 시 GitHub Actions가 이미지 빌드→GHCR→매니페스트 레포 태그 갱신(GitOps)로 배포된다. 문제가 생기면 `.github/workflows/rollback.yml`로 커밋 SHA를 지정해 롤백.
- 커밋 메시지는 한국어 + Conventional Commits 접두사(`feat`, `fix`, `refactor`, `chore`)를 사용한다.

## 참고 문서

- `docs/README.md` — docs 전체 인덱스 (아래 문서들의 지도)
- `docs/observability/` — 관측성 작업 로그(`observability.md`) + 트레이싱 커버리지(`tracing-coverage.md`)
- `docs/domain/` — 도메인별 설계 (`reward-domain`, `report-api`, `feedback-api`, `battle-vote-guest`, `badge-design`, `swipe-battle-frontend`)
- `docs/chaos/` — 장애 주입 & RCA 런북 (`README.md`부터)
- `k6/` — 부하/스모크 테스트 스크립트
