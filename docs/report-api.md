# 신고 API (피드 / 배틀)

부적절한 피드/배틀을 신고하는 API. 피드와 배틀은 **별도 테이블**(`tb_feed_report`, `tb_battle_report`)로 분리되어 있으며, 각각 `BaseReport`(`@MappedSuperclass`)로 공통 필드를 공유한다.

- 신고는 **로그인 사용자만 가능**
- **본인이 작성한 콘텐츠는 신고 불가**
- 동일 콘텐츠에 대해 **사용자당 1회만 가능** (DB unique constraint로 보장)
- 어드민 조회/처리 API는 현재 스코프 아님

---

## 1. 피드 신고

| | |
|---|---|
| Method | `POST` |
| Path | `/feeds/{feedId}/reports` |
| 인증 | **필수** (`Authorization: Bearer <token>`) |

### Path Parameters

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `feedId` | number | 신고할 피드 ID |

### Request Body

```json
{
  "reason": "SPAM",
  "detail": "광고성 댓글이 반복됩니다."
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|---|---|---|---|---|
| `reason` | enum | O | `ReportReason` 중 하나 | 신고 사유 |
| `detail` | string | X | max **500자** | 상세 내용 (자유 입력) |

### Response

```json
{
  "success": true,
  "message": "신고가 접수되었습니다.",
  "data": 1234
}
```

`data`는 생성된 신고 ID.

### 에러

| 상황 | HTTP | errorCode | message |
|---|---|---|---|
| 피드 없음 | 404 | `FEED_NOT_FOUND` | 피드를 찾을 수 없습니다. |
| 본인 피드 신고 | 400 | `FEED_REPORT_SELF` | 본인의 피드는 신고할 수 없습니다. |
| 중복 신고 | 409 | `FEED_REPORT_DUPLICATED` | 이미 신고한 피드입니다. |
| 사유 누락 | 400 | `VALIDATION_ERROR` | 신고 사유는 필수입니다. |

---

## 2. 배틀 신고

| | |
|---|---|
| Method | `POST` |
| Path | `/battles/{battleId}/reports` |
| 인증 | **필수** |

### Path Parameters

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `battleId` | number | 신고할 배틀 ID |

### Request Body

```json
{
  "reason": "SEXUAL",
  "detail": "음란한 이미지가 포함되어 있습니다."
}
```

스키마는 피드 신고와 동일 (`reason` 필수, `detail` 선택, 500자 이하).

### Response

```json
{
  "success": true,
  "message": "신고가 접수되었습니다.",
  "data": 5678
}
```

### 에러

| 상황 | HTTP | errorCode | message |
|---|---|---|---|
| 배틀 없음 | 404 | `BATTLE_NOT_FOUND` | 배틀을 찾을 수 없습니다. |
| 본인 배틀 신고 | 400 | `BATTLE_REPORT_SELF` | 본인의 배틀은 신고할 수 없습니다. |
| 중복 신고 | 409 | `BATTLE_REPORT_DUPLICATED` | 이미 신고한 배틀입니다. |

---

## 3. 신고 사유 (`ReportReason`)

| 값 | 설명 |
|---|---|
| `SPAM` | 광고/스팸 |
| `OFFENSIVE` | 욕설/비방 |
| `SEXUAL` | 음란/선정성 |
| `VIOLENCE` | 폭력/혐오 |
| `MISINFORMATION` | 거짓 정보 |
| `COPYRIGHT` | 저작권 침해 |
| `ETC` | 기타 |

요청 시 `reason: "SPAM"` 형태로 enum 키를 그대로 전송한다.

---

## 4. 처리 상태 (`ReportStatus`)

저장된 신고 레코드의 상태 필드(어드민 처리용 — 현재는 API 미노출).

| 값 | 설명 |
|---|---|
| `PENDING` | 접수 (기본값) |
| `REVIEWED` | 검토중 |
| `RESOLVED` | 처리됨 |
| `REJECTED` | 반려 |

---

## 5. 데이터 모델

### `tb_feed_report`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | bigint PK | 신고 ID |
| `feed_id` | bigint FK → `tb_feed` | 신고 대상 피드 |
| `reporter_id` | bigint | 신고자 ID |
| `reason` | varchar(30) | `ReportReason` |
| `detail` | varchar(500) | 상세 내용 (nullable) |
| `status` | varchar(20) | `ReportStatus` (default `PENDING`) |
| `created_at` | datetime | 접수 시각 |
| `updated_at` | datetime | 마지막 갱신 시각 |

- Unique: `(feed_id, reporter_id)` — 중복 신고 차단
- Index: `feed_id`, `(status, created_at DESC)`

### `tb_battle_report`

`tb_feed_report`와 스키마 동일 (FK만 `battle_id` → `tb_battle`).

- Unique: `(battle_id, reporter_id)`
- Index: `battle_id`, `(status, created_at DESC)`

---

## 6. 프론트 적용 가이드

- 신고 버튼은 **로그인 사용자에게만 노출**.
- 본인 콘텐츠에는 신고 버튼 자체를 숨기는 게 자연스러움 (서버에서도 차단되지만 UX 차원).
- 사유 선택 UI는 `ReportReason` 7개를 라디오/리스트로 노출. `ETC` 선택 시 `detail` 입력 필드를 활성화하면 좋음.
- 중복 신고(`FEED_REPORT_DUPLICATED` / `BATTLE_REPORT_DUPLICATED`) 응답 시 "이미 신고하셨습니다" 안내.
- 신고 접수 후 처리 결과 알림은 현재 미구현. 추후 `ReportStatus` 변경 시 알림이 추가될 예정.
