# 스와이프 배틀 프론트엔드 통합 가이드

투표(SINGLE/MULTIPLE) 외에 `VoteType.SWIPE` 신규 도입. 배틀별로 voteType이 다르므로 화면 진입 시 분기 필수.

---

## 1. 요구사항

- 3단계 스와이프: **강추 PICK**(위 ⬆️) / **PICK**(오른쪽 ➡️) / **PASS**(왼쪽 ⬅️)
- **비로그인(게스트)도 참여 가능** — userId 또는 guestId 중 하나로 식별
- **중단/재개 지원** — 20개 중 10개만 하고 나가도 OK. 다음 진입 시 안 한 10개부터
- **재호출은 멱등 덮어쓰기** — 같은 voter가 같은 아이템에 다시 보내면:
  - 동일 verdict → 서버 no-op (응답만 정상)
  - 다른 verdict → 마지막 verdict로 덮어쓰기. 점수는 마지막 1회만 반영 (중복 가산 없음)
- 일반 흐름에선 미진행 정의(`/swipe/next`에서 안 보임) 그대로라 재호출이 잘 발생하지 않지만, 결과 화면에서 verdict 변경 UI를 두면 활용 가능
- 결과는 단순 합산 (`strong*3 + pick*1`) 내림차순 랭킹

---

## 2. 비즈니스 로직

### 점수/랭킹
| Verdict | 점수 |
|---|---|
| `STRONG_PICK` | +3 |
| `PICK` | +1 |
| `PASS` | 0 (저장은 됨 — 재진입 시 안 보이게 하려고) |

랭킹 점수 = `strongPickCount * 3 + pickCount * 1`. 동점이면 id asc.

### 진행 상태
- `completedCount` = voter가 해당 배틀에서 스와이프한 row 수 (PASS 포함)
- `totalCount` = 활성 아이템 전체 수
- 미진행 = `total - completed`
- **별도 progress 테이블 없이** swipe row 존재 여부로 표현

### 식별 (voter)
- 로그인 사용자: `Authorization` 헤더 등 기존 방식 그대로
- 게스트: 기존 vote에서 쓰던 쿠키/localStorage 기반 `guestId` 그대로 사용 (별도 작업 없음)
- 캐시 삭제하면 새 guestId 발급되어 **처음부터 다시** (불가피, 의도된 한계)

### 배틀 생성
- 생성 시 `voteType` 선택지에 `SWIPE` 추가 — 배틀별로 SINGLE/MULTIPLE/SWIPE 선택
- 기존 진행 중 배틀은 그대로 vote로 동작 (점진 전환)

---

## 3. API 연동

### 3-1. 다음 스와이프 아이템 조회

```http
GET /battles/{battleId}/swipe/next?size=10
```

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
      },
      ...
    ],
    "completedCount": 5,
    "totalCount": 20
  }
}
```

- 랜덤 순서로 반환됨
- `items`가 빈 배열이면 전부 완료 → 결과 화면으로
- `size` 미지정 시 기본 10

### 3-2. 스와이프 1건 등록

```http
POST /battles/{battleId}/swipe
Content-Type: application/json

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
  },
  "message": "스와이프가 등록되었습니다."
}
```

**저장 단위는 1건/요청**. 빠른 인터랙션이라 클라이언트가 batching 하고 싶으면 추후 batch 엔드포인트 추가 검토.

### 3-3. 결과(랭킹) 조회

```http
GET /battles/{battleId}/swipe/result
```

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
      },
      ...
    ]
  }
}
```

### 3-4. 배틀 상세/아이템 조회 (기존 + swipeStats 추가)

`GET /battles/{battleId}` 와 `GET /battles/{battleId}/items` 응답의 `BattleItemInfo`에 **`swipeStats` 필드가 신설**:

```json
{
  "id": 10,
  "displayName": "맥북 M5",
  "imageUrl": "https://...",
  "voteCount": 0,        // SWIPE 배틀에선 사용 안 함
  "totalScore": 0,
  "rank": 1,             // swipeStats 있으면 swipe 점수 기준으로 부여됨
  "swipeStats": {        // SWIPE 배틀에서만 채워짐, vote 배틀은 null
    "strongPickCount": 42,
    "pickCount": 18,
    "passCount": 7,
    "score": 144
  }
}
```

- `swipeStats != null` → SWIPE 배틀 → swipe UI/통계로 표시
- `swipeStats == null` → vote 배틀 → 기존 vote UI 그대로

> **클라이언트 분기 방법**: `battle.voteType === "SWIPE"`로 분기해도 되고, 아이템 단위로 `swipeStats != null`로 분기해도 됨. 응답 한 번에 다 들어오므로 추가 호출 불필요.

---

## 4. 화면 구성

### 4-1. 배틀 진입 (분기)

```
GET /battles/{id}
 └─ battle.voteType === "SWIPE" ?
     ├─ YES → 스와이프 화면으로 (아래 4-2)
     └─ NO  → 기존 투표 화면 그대로
```

### 4-2. 스와이프 진행 화면

**구성:**
- 카드 스택 (현재 카드 + 다음 2~3장 미리 렌더)
- 상단 진행률: `▓▓▓▓▓░░░░░  5/20` (`completedCount/totalCount`)
- 카드 위 스와이프 매핑:
  - **위 ⬆️ → STRONG_PICK** (불꽃🔥/금색 강조, 진동 강하게)
  - **오른쪽 ➡️ → PICK** (하트, 진동 가볍게)
  - **왼쪽 ⬅️ → PASS** (X, 진동 없음)
- 카드 콘텐츠:
  - `imageUrl` (CUSTOM은 사용자 업로드, PRODUCT는 상품 이미지)
  - `displayName`
  - `itemType === "YOUTUBE"`이면 `embedUrl`로 영상 임베드

**플로우:**
1. 진입 시 `GET /swipe/next?size=10`
2. 스와이프 → `POST /swipe` (낙관적 UI: 응답 안 기다리고 다음 카드 노출)
3. 남은 카드 3장 이하로 떨어지면 백그라운드로 `GET /swipe/next` 다시 호출 → 카드 보충
4. `items`가 빈 배열로 오면 → "끝났어요! 결과 보기" CTA

### 4-3. 중단/재개

- 사용자가 도중에 나가도 별도 저장 액션 불필요 (1건씩 즉시 저장됨)
- 재진입 시 자동으로 미진행 카드부터 보임 (`/swipe/next`가 알아서 처리)
- 진입 시 `completedCount > 0`이면 "이어하기" UX 가능: "5/20 진행 중, 이어할까요?"

### 4-4. 결과 화면

**구성:**
- 진행률이 100%면 자동 진입 가능, 진행 중에도 "순위 보기" 버튼으로 미리 볼 수 있음
- `GET /swipe/result`로 랭킹 받기
- 각 아이템 카드: 순위, 이미지, 이름, "강추 N · PICK N · PASS N" + 총 점수
- 1~3위는 강조 (메달 등)

### 4-5. 배틀 생성 화면

- voteType 선택지에 `SWIPE("스와이프(강추/PICK/PASS)")` 추가
- 기존 SINGLE("1인 1표") / MULTIPLE("1인 3표")과 나란히

---

## 5. 에러 처리

| HTTP | 에러 코드 | 의미 | 권장 처리 |
|---|---|---|---|
| 400 | `SWIPE_NOT_ALLOWED` | SWIPE 배틀이 아닌데 swipe API 호출 | voteType 분기 누락 — 클라이언트 버그. 토스트 |
| 400 | `VOTE_NOT_ALLOWED_FOR_SWIPE` | SWIPE 배틀에 기존 vote API 호출 | 클라이언트 분기 누락 — 버그 |
| 400 | `CANNOT_VOTE_ITEM` | 아이템이 비활성/삭제됨 | 다음 카드로 이동 |
| 404 | `BATTLE_NOT_FOUND` / `BATTLE_ITEM_NOT_FOUND` | ID 잘못됨 | 일반 에러 처리 |

> 중복 클릭/네트워크 재시도로 같은 verdict가 두 번 와도 서버에서 멱등 처리되므로 별도 클라이언트 가드 불필요.

---

## 6. 프론트 수정 체크리스트

### 신규 화면
- [ ] 스와이프 진행 화면 (카드 스택 + 위/우/좌 매핑 + 진행률)
- [ ] 스와이프 결과 화면 (랭킹 리스트)

### 기존 화면 수정
- [ ] **배틀 진입 분기**: `voteType === "SWIPE"`면 스와이프 화면 라우팅
- [ ] **배틀 생성 폼**: voteType 선택지에 `SWIPE` 추가
- [ ] **배틀 상세 (`GET /battles/{id}`)**: `swipeStats != null` 일 때 결과 표시를 vote 통계 대신 swipe 통계로
- [ ] **배틀 아이템 목록 (`GET /battles/{id}/items`)**: 같은 처리 (응답 동일)
- [ ] **랭킹 표시**: `rank`는 voteType 무관하게 그대로 사용 가능 (서버가 SWIPE면 swipe 점수 기준 부여)

### 공통
- [ ] 게스트 ID 관리는 기존 vote에서 쓰던 방식 그대로 (별도 작업 없음)
- [ ] 강추/PICK/PASS 아이콘·색상·진동 강도 차등 (UX 가이드)
  - 강추: 🔥 금색, 진동 강
  - PICK: ❤️ 기본색, 진동 약
  - PASS: ✖️ 회색, 진동 없음

### 자동/수동 테스트 케이스
- [ ] 스와이프 1건 후 새로고침 → 해당 카드 미노출
- [ ] 10/20 진행 후 재진입 → 안 한 10개부터 노출
- [ ] 같은 아이템에 빠른 더블 탭 → 첫 건만 반영, 두 번째는 `SWIPE_ALREADY_DONE` 조용히 무시
- [ ] 비로그인 → 게스트로 정상 동작, 로그인 후엔 별개 진행 (게스트/유저 ID 다름)
- [ ] vote 배틀 페이지에 스와이프 API 안 가는지 (분기 누락 방지)

---

## 7. 향후 확장 (백엔드 후속 PR)

다음은 1차 백엔드 PR에 포함되지 않은 것들 — 프론트도 일단은 신경 안 써도 됨:

- 핫스코어 계산이 swipe 통계도 반영하도록
- 배틀 종료 알림(`BATTLE_RESULT`)이 swipe 결과 1위를 포함하도록
- batch swipe POST 엔드포인트 (속도 최적화가 필요할 때)
