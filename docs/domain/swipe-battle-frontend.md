# 스와이프 배틀 프론트엔드 통합 가이드

배틀의 새 `VoteType.SWIPE` 도입. 배틀별로 `voteType`이 다르므로 화면 진입 시점에 분기 필수.

---

## 1. 개요

- **3단계 verdict**: 강추 PICK(위 ⬆️) / PICK(오른쪽 ➡️) / PASS(왼쪽 ⬅️)
- **비로그인도 참여 가능** — 로그인은 `userId`, 비로그인은 쿠키 `gid`(서버 자동 발급, 1년 TTL)
- **중단/재개** — 매 스와이프마다 1건씩 서버 저장. 재진입 시 안 한 것부터
- **재스와이프 가능** — 다 끝난 뒤에도 next는 active 전체를 다시 돌려줌
- **upsert/멱등 정책** — 같은 `(voter, item)`에 다시 보내면 마지막 verdict로 덮어쓰기. 점수 이중 가산 없음
- **랭킹** — 단순 합산 `strongPickCount*3 + pickCount*1` 내림차순 (PASS는 0점)

---

## 2. 도메인 모델

### 2-1. Verdict와 점수

| Verdict | 점수 | 용도 |
|---|---|---|
| `STRONG_PICK` | +3 | 강추 |
| `PICK` | +1 | 약한 긍정 |
| `PASS` | 0 | 부정 (저장은 됨 — 다음 진입 시 안 보이도록) |

랭킹 = `strongPickCount * 3 + pickCount * 1` 내림차순. 동점이면 itemId 오름차순.

### 2-2. 진행 상태

- `completedCount` = voter가 해당 배틀에서 스와이프한 row 수 (PASS 포함)
- `totalCount` = 활성 아이템 수
- 미진행 = `totalCount - completedCount`
- 별도 progress 테이블 없이 swipe row 존재 여부로 추적

### 2-3. 멱등 upsert

`POST /swipe`는 항상 upsert:

| 상태 | 동작 |
|---|---|
| 기존 row 없음 | INSERT + 새 verdict 카운터 +1 |
| 기존 row 있고 **동일 verdict** | no-op (응답만 정상) |
| 기존 row 있고 **다른 verdict** | 기존 카운터 -1, 새 카운터 +1, row의 verdict 갱신 |

→ 중복 클릭/네트워크 재시도/명시적 verdict 변경 모두 안전.

---

## 3. Voter 식별

- **로그인**: 기존 `Authorization` 헤더 방식 그대로
- **비로그인(게스트)**: 쿠키 `gid` (HttpOnly + SameSite=Lax + 1년 TTL). 서버가 없으면 자동 발급해 `Set-Cookie`로 내려줌
- 프론트는 별도 작업 없음. `credentials: 'include'` 등으로 쿠키만 함께 보내면 됨
- **한계**: 캐시 삭제 시 새 `gid` 발급 → 처음부터 (의도된 동작, 어뷰징 사후 추적은 별도 IP 로그)

---

## 4. API 참조

### 4-1. `GET /battles/{battleId}/swipe/next?size=10`

다음 스와이프할 아이템을 랜덤으로 반환. 미진행이 0이면 active 전체를 fallback으로 돌려줌(재스와이프용).

응답:
```json
{
  "data": {
    "items": [
      {
        "id": 10,
        "itemType": "CUSTOM",
        "displayName": "맥북 M5",
        "imageUrl": "https://...",
        "embedUrl": null
      }
    ],
    "completedCount": 5,
    "totalCount": 20
  }
}
```

- `size` 기본 10
- **"끝났음" 판단**: `completedCount === totalCount`
- 끝났어도 `items`엔 active 전체가 옴 → 재스와이프 가능. 점수 이중 가산은 upsert 정책으로 차단

### 4-2. `POST /battles/{battleId}/swipe`

스와이프 1건 등록(upsert).

요청:
```json
{
  "itemId": 10,
  "verdict": "STRONG_PICK"   // STRONG_PICK | PICK | PASS
}
```

응답:
```json
{
  "data": {
    "itemId": 10,
    "completedCount": 6,
    "totalCount": 20
  }
}
```

- 1건/요청. batch 필요해지면 후속 PR로 별도 엔드포인트 추가 검토

### 4-3. `GET /battles/{battleId}/swipe/result`

랭킹 결과. 가벼운 결과만 필요할 때(공유 페이지, 위젯 등) 사용.

응답:
```json
{
  "data": {
    "items": [
      {
        "rank": 1,
        "id": 12,
        "displayName": "맥북 M5",
        "imageUrl": "https://...",
        "strongPickCount": 42,
        "pickCount": 18,
        "passCount": 7,
        "score": 144
      }
    ]
  }
}
```

### 4-4. `GET /battles/{battleId}` / `GET /battles/{battleId}/items` (기존 확장)

`BattleItemInfo` 응답에 SWIPE 통계가 함께 들어옴(SWIPE 배틀만):

```json
{
  "id": 10,
  "displayName": "맥북 M5",
  "voteCount": 0,
  "totalScore": 0,
  "rank": 1,
  "swipeStats": {
    "strongPickCount": 42,
    "pickCount": 18,
    "passCount": 7,
    "score": 144
  }
}
```

- `swipeStats != null` → SWIPE 배틀 → swipe 통계로 표시
- `swipeStats == null` → vote 배틀 → 기존 vote 통계로 표시
- `rank`는 voteType과 무관하게 그대로 사용 — 서버가 SWIPE면 swipe 점수 기준으로 부여

> 배틀 상세 한 번 호출로 결과 + 통계 + 랭킹이 다 들어오므로, 결과 화면에서 굳이 `/swipe/result`를 또 부를 필요는 없음.

### 4-5. `POST /battles` (배틀 생성)

`voteType` 필드에 `SWIPE` 그대로 사용 가능. DTO 변경 없음.

```json
{
  "title": "...",
  "voteType": "SWIPE",
  "items": [...]
}
```

---

## 5. 화면 흐름

### 5-1. 배틀 진입 분기

```
GET /battles/{id}
 └─ battle.voteType
     ├─ "SWIPE" → 스와이프 화면 (5-2)
     └─ 그 외   → 기존 투표 화면
```

### 5-2. 스와이프 진행 화면

**UI:**
- 카드 스택 (현재 카드 + 다음 2~3장 미리 렌더)
- 진행률 바: `5/20`
- 스와이프 매핑:
  - 위 ⬆️ → STRONG_PICK (🔥 금색, 진동 강)
  - 오른쪽 ➡️ → PICK (❤️ 기본색, 진동 약)
  - 왼쪽 ⬅️ → PASS (✖️ 회색, 진동 없음)
- 카드 콘텐츠:
  - `imageUrl` (CUSTOM/PRODUCT/YOUTUBE 공통)
  - `displayName`
  - `itemType === "YOUTUBE"`이면 `embedUrl`로 임베드

**플로우:**
1. 진입 시 `GET /swipe/next?size=10`
2. 스와이프 → 낙관적 UI로 다음 카드 노출, 비동기로 `POST /swipe`
3. 남은 카드 3장 이하면 백그라운드 `GET /swipe/next`로 보충
4. `completedCount === totalCount`가 되는 순간 → "끝났어요! 결과 보기" CTA

### 5-3. 중단 후 재진입

- 별도 저장 액션 불필요 (1건씩 즉시 저장)
- 진입 시 `GET /swipe/next` 호출 → 안 한 카드부터 자동 노출
- `completedCount > 0`이면 "이어하기" UX 가능 ("5/20 진행 중, 이어할까요?")

### 5-4. 결과 화면

- 자동 진입: 진행 중에 `completedCount === totalCount`가 된 직후
- 수동 진입: 진행 중 "순위 보기" 버튼
- 표시: `BattleItemInfo.swipeStats` 또는 `/swipe/result`의 ranked 리스트
- 카드: 순위, 이미지, 이름, "강추 N · PICK N · PASS N · 점수 X"
- 1~3위는 메달 강조

### 5-5. 결과 화면에서 재스와이프

- "다시 스와이프하기" 버튼 → `/swipe/next` 호출하면 active 전체가 옴 (다 끝난 voter라도)
- verdict 변경하면 upsert로 마지막 verdict만 반영
- 진행률은 이미 100%지만 "수정 모드"로 표기 가능

### 5-6. 배틀 생성 폼

- voteType 선택지에 `SWIPE("스와이프(강추/PICK/PASS)")` 추가
- 기존 `SINGLE("1인 1표")` / `MULTIPLE("1인 3표")`과 나란히

---

## 6. 에러 처리

| HTTP | 에러 코드 | 의미 | 권장 처리 |
|---|---|---|---|
| 400 | `SWIPE_NOT_ALLOWED` | SWIPE 배틀이 아닌데 swipe API 호출 | 클라이언트 분기 누락 (버그). 토스트 |
| 400 | `VOTE_NOT_ALLOWED_FOR_SWIPE` | SWIPE 배틀에 기존 vote API 호출 | 클라이언트 분기 누락 (버그) |
| 400 | `CANNOT_VOTE_ITEM` | 아이템이 비활성/삭제됨 | 다음 카드로 이동 |
| 404 | `BATTLE_NOT_FOUND` / `BATTLE_ITEM_NOT_FOUND` | ID 잘못됨 | 일반 에러 처리 |

> 중복 클릭/네트워크 재시도로 같은 verdict가 두 번 와도 서버가 멱등 처리 → 별도 클라이언트 가드 불필요.

---

## 7. 프론트 수정 체크리스트

### 신규 화면
- [ ] 스와이프 진행 화면 (카드 스택 + 위/우/좌 매핑 + 진행률)
- [ ] 스와이프 결과 화면 (랭킹 + "다시 스와이프하기" 옵션)

### 기존 화면 수정
- [ ] **배틀 진입**: `voteType === "SWIPE"`면 스와이프 화면 라우팅
- [ ] **배틀 생성 폼**: voteType 선택지에 `SWIPE` 추가
- [ ] **배틀 상세 / 아이템 목록**: `swipeStats != null` 일 때 vote 통계 대신 swipe 통계 표시
- [ ] **랭킹 표시**: `rank` 그대로 사용 (서버가 voteType별로 알아서 부여)

### 공통
- [ ] 게스트 식별은 기존 vote 패턴 그대로 (`credentials: 'include'`)
- [ ] verdict별 아이콘·색·진동 차등 (🔥금/❤️기본/✖️회색)

### 테스트 케이스
- [ ] 스와이프 1건 후 새로고침 → 해당 카드 미노출
- [ ] 10/20 진행 후 재진입 → 안 한 10개부터 노출
- [ ] 전부 완료 후 next 호출 → active 전체 다시 옴
- [ ] 같은 아이템 다시 스와이프 (다른 verdict) → 점수 중복 가산 없음
- [ ] 비로그인 → 게스트로 정상 동작, 로그인 후엔 별개 voter
- [ ] vote 배틀 페이지에서 스와이프 API 안 가는지

---

## 8. 향후 확장 (후속 PR — 프론트는 지금 신경 안 써도 됨)

- batch swipe POST 엔드포인트 (UX 최적화 필요 시)
- 결과 화면에서 verdict 변경 UI (현재 정책은 멱등 upsert로 백엔드 준비 완료)

### 백엔드 완료 (참고)

- **핫스코어** — `Battle.totalSwipes`가 신규 swipe 시점에 +1, `baseScore`에 `totalSwipes * 0.1`로 가산 (가벼운 신호 — 풀완주 시 view 수준으로만 기여)
- **종료 알림** — SWIPE 배틀에선 1위 아이템명이 포함된 `BATTLE_RESULT_WITH_WINNER`로 발송 ("[배틀명] 배틀 종료! 1위는 [아이템명] 🏆 결과를 확인해보세요!"). 점수 0(아무도 스와이프 안 한 경우)이면 기존 `BATTLE_RESULT` 메시지로 폴백
