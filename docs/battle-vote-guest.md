# 배틀 게스트 투표 / 투표 변경

비로그인 사용자도 배틀에 투표할 수 있도록 확장하고, SINGLE/MULTIPLE 모두 **재투표(투표 변경)** 를 허용한다. 참여율 확보가 우선이라 게스트 어뷰징(쿠키 삭제 후 재투표)은 의도적으로 수용한다.

- 게스트 투표는 **쿠키(`gid`) 기반**으로 식별
- **EXP 적립은 로그인 사용자에게만** 지급
- 같은 투표자(로그인/게스트)가 다시 투표하면 **기존 표를 갈아엎고 새 표 반영**

관련 커밋: `dd7897c` (게스트 투표 도입), `2c6f6a3` (투표 변경 허용), `944416e` (재투표 시 unique 제약 충돌 수정)
DB 마이그레이션: `docs/migrations/2026-05-07_battle_vote_guest.sql`

---

## 1. API

| | |
|---|---|
| Method | `POST` |
| Path | `/battles/{battleId}/vote` |
| 인증 | **선택** — 비로그인도 호출 가능 |
| 쿠키 | 비로그인이면 응답 시 `gid` 쿠키 자동 발급/재사용 |

### Request Body

SINGLE 배틀:
```json
{ "votes": [ { "itemId": 10, "rank": 1 } ] }
```

MULTIPLE 배틀:
```json
{ "votes": [
  { "itemId": 10, "rank": 1 },
  { "itemId": 20, "rank": 2 },
  { "itemId": 30, "rank": 3 }
] }
```

### Response

```json
{
  "data": {
    "gainedExp": 10,
    "capped": false
  },
  "message": "투표가 완료되었습니다."
}
```

- 게스트는 EXP 미지급 → `gainedExp: 0`
- 로그인 사용자라도 일일/누적 cap에 걸리면 `capped: true`

---

## 2. 투표자 식별 (`VoterId`)

`@CurrentVoterId VoterId voter` 파라미터로 컨트롤러에서 받는다.

| 케이스 | `voter.userId` | `voter.guestId` |
|---|---|---|
| 로그인 (JWT 유효) | 사용자 ID | `null` |
| 비로그인 + 기존 `gid` 쿠키 보유 | `null` | 쿠키값 (UUID) |
| 비로그인 + 쿠키 없음 | `null` | 신규 발급 UUID (응답 Set-Cookie) |

`CurrentVoterIdArgumentResolver`가 인증 우선, 실패 시 쿠키 폴백.

### `gid` 쿠키 옵션

| 속성 | 값 |
|---|---|
| HttpOnly | `true` (JS 접근 차단) |
| SameSite | `Lax` |
| Secure | HTTPS 환경에서만 |
| Path | `/` |
| Max-Age | 365일 |

> 같은 사이트(eTLD+1) 안에서 프론트와 API가 함께 서비스되는 구성 기준. 다른 도메인 간 호출 시엔 `SameSite=None; Secure`로 변경 필요.

---

## 3. 재투표 동작

SINGLE/MULTIPLE 모두 동일한 흐름으로 통합되었다.

```
기존 표 조회 → 통계 롤백(참여자수/총투표수/아이템 점수) → 기존 표 DELETE
            → 새 표 INSERT → 통계 재반영
```

| 모드 | 검증 | 재투표 |
|---|---|---|
| SINGLE | rank=1, 1개 항목만 | 허용 (기존 표 갈아엎음) |
| MULTIPLE | rank가 1·2·3 연속 | 허용 (기존 표 갈아엎음) |

기존 `BattleErrorCode.ALREADY_VOTED`는 사용처가 사라져 제거.

### EXP 재지급 여부
`ExpGrantService.grant(userId, source, sourceId)`가 `(user, source, sourceId)` 단위 멱등이면 재투표 시 추가 지급되지 않음. 게스트는 어떤 경우에도 0.

---

## 4. DB 변경

### 컬럼 / 제약

| 항목 | 변경 |
|---|---|
| `tb_battle_vote.user_id` | `NULL` 허용 (게스트는 user_id NULL) |
| `tb_battle_vote.guest_id` | `VARCHAR(36) NULL` 추가 |
| `uk_battle_user_rank` | `(battle_id, user_id, vote_rank)` 유니크 |
| `uk_battle_guest_rank` | `(battle_id, guest_id, vote_rank)` 유니크 |
| `chk_battle_vote_voter` | `user_id IS NOT NULL OR guest_id IS NOT NULL` |

`ddl-auto=update`는 unique/check 제약을 추가하지 않으므로 마이그레이션 SQL 수동 적용 필요.

### Hibernate flush 순서 이슈

`Battle.votes`가 `@OneToMany ... orphanRemoval=true` 인데 Hibernate 기본 ActionQueue는
**INSERT → UPDATE → DELETE** 순으로 실행한다. 따라서 같은 트랜잭션에서 `deleteAll(old)` → `saveAll(new)` 를 호출해도, flush 시점엔 새 표 INSERT가 먼저 나가 unique 제약과 충돌:

```
Duplicate entry '14-7-1' for key 'tb_battle_vote.uk_battle_user_rank'
```

`removeExistingVotesIfPresent` 마지막에 `battleVoteRepository.flush()`를 호출하여
DELETE를 즉시 DB에 보내고, 이후 INSERT가 빈 자리에 들어가도록 수정.

---

## 5. 프론트 통합 가이드

### 1) 투표 버튼 게이팅 제거
비로그인 사용자에게도 그대로 노출. "로그인하시겠어요?" 강제 X.

### 2) 쿠키 송수신 활성화 (필수)
```ts
fetch(`/battles/${id}/vote`, {
  method: 'POST',
  credentials: 'include',     // ← gid 쿠키 송수신
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(req),
});
// axios: axios.defaults.withCredentials = true
```

### 3) 재투표 UX
이미 투표 이력이 있는 상태(서버에서 내려준 `userVote` 등)에서 다시 투표하려 하면 **확인 모달** 띄움. 같은 엔드포인트로 재호출하면 서버가 알아서 갈아엎음.

### 4) EXP 토스트 분기
- `gainedExp > 0` → "EXP +N" 토스트
- 게스트(`gainedExp === 0`)에는 "로그인하면 EXP를 받을 수 있어요" CTA 노출 (선택)

---

## 6. 의도적으로 수용한 한계

| 한계 | 영향 | 대응 |
|---|---|---|
| 게스트가 쿠키 삭제 후 재투표 | 표 N배 증가 가능 | 미대응 — 참여율이 우선. 가입 유도로 보완 |
| 게스트는 EXP 없음 | 보상 동기 약화 | 의도된 정책 (어뷰징 방지) |
| 다른 디바이스/브라우저는 다른 게스트로 인식 | 표 분산 | 정상 동작 — 동일 사용자의 의도적 다중 투표는 막지 못함 |
