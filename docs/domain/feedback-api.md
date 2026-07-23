# 의견 보내기 API

사용자 의견(피드백)을 받기 위한 단순 게시판형 API.

- 등록은 **누구나 가능** (비회원 포함, 인증 헤더 불필요)
- 목록 조회는 **관리자 전용** (`ROLE_ADMIN`)
- 답변/수정/삭제 기능은 현재 없음 (받기만)

---

## 1. 의견 등록

| | |
|---|---|
| Method | `POST` |
| Path | `/feedbacks` |
| 인증 | 불필요 |

### Request Body

```json
{
  "title": "버튼 클릭이 안돼요",
  "content": "메인 화면 우측 상단 알림 버튼 클릭 시 반응이 없습니다."
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|---|---|---|---|---|
| `title` | string | O | NotBlank, max **100자** | 제목 |
| `content` | string | O | NotBlank, max **2000자** | 내용 |

### Response

```json
{
  "success": true,
  "message": "의견이 등록되었습니다."
}
```

### 검증 실패 예시

```json
{
  "success": false,
  "message": "제목은 필수입니다",
  "errorCode": "VALIDATION_ERROR"
}
```

---

## 2. 의견 목록 조회 (관리자)

| | |
|---|---|
| Method | `GET` |
| Path | `/feedbacks` |
| 인증 | **관리자 토큰 필수** (`Authorization: Bearer <admin-token>`) |

### Query Parameters (페이징)

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `page` | int | `0` | 페이지 번호 (0-base) |
| `size` | int | `20` | 페이지 크기 |
| `sort` | string | `createdAt,desc` | 정렬 (`필드,방향`) |

예: `GET /feedbacks?page=0&size=20`

### Response

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 12,
        "title": "버튼 클릭이 안돼요",
        "content": "메인 화면 우측 상단 알림 버튼 클릭 시 반응이 없습니다.",
        "createdAt": "2026-04-29T14:21:08"
      },
      {
        "id": 11,
        "title": "다크모드 추가 요청",
        "content": "야간에 눈이 부셔서 다크모드가 있으면 좋겠어요.",
        "createdAt": "2026-04-29T11:02:33"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": { "sorted": true, "unsorted": false, "empty": false }
    },
    "totalElements": 42,
    "totalPages": 3,
    "first": true,
    "last": false,
    "number": 0,
    "numberOfElements": 20,
    "empty": false
  }
}
```

| 필드 (content[]) | 타입 | 설명 |
|---|---|---|
| `id` | number | 의견 ID |
| `title` | string | 제목 |
| `content` | string | 내용 |
| `createdAt` | string (ISO-8601) | 등록 일시 |

### 권한 부족 시 (비관리자)

```json
{
  "success": false,
  "message": "권한이 없습니다.",
  "errorCode": "FORBIDDEN"
}
```

---

## 3. 프론트 적용 가이드

- 등록 폼: `title`, `content` 두 필드만. 글자수 카운터 권장 (100 / 2000).
- 등록 버튼은 **미로그인 상태에서도 활성화**. 별도 토큰 첨부 불필요.
- 관리자 대시보드 목록은 `Page` 응답 구조를 그대로 사용. 정렬은 `createdAt DESC` 기본.
- 답변 노출 UI는 현재 스코프 아님. 추후 답변 기능이 추가되면 응답에 `replyContent`, `repliedAt` 필드가 추가될 예정.
