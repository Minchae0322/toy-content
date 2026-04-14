# 보상 시스템 도메인

## 개요

사용자 참여를 유도하기 위한 보상 시스템으로, EXP/레벨, 뱃지, 일일 미션, 스트릭, 카테고리 숙련도, 배틀 예측 등으로 구성된다.

---

## 엔티티

### Badge (뱃지 마스터)

> 테이블: `tb_badge`

뱃지 정의 마스터 테이블. 뱃지의 종류와 메타데이터를 관리한다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| badge_id | Long (PK) | 뱃지 ID |
| code | String(60) | 뱃지 코드 (UK). `BadgeCode` enum 참조 |
| name | String(80) | 뱃지 이름 (예: 구매처 쉐어러) |
| description | String(300) | 뱃지 설명 / 획득 조건 문구 |
| icon_emoji | String(10) | 아이콘 이모지 (예: 🏪) |
| icon_image_url | String(500) | 뱃지 이미지 URL |
| category | String(30) | 카테고리 (BRAG/CURATION/STREAK/SEASON) |
| is_seasonal | Boolean | 시즌 한정 뱃지 여부 (default: false) |
| season_code | String(20) | 시즌 코드 (시즌 한정일 경우) |
| activated | Boolean | 활성 여부 (default: true) |

#### BadgeCode (뱃지 코드 카탈로그)

코드에서 참조용으로 사용하는 enum. DB에는 String으로 저장된다.

```java
public enum BadgeCode {
    BUY_PLACE_SHARER,  // 구매처 쉐어러
    HOT_MAKER;         // 핫 메이커
}
```

---

### UserBadge (사용자 뱃지)

> 테이블: `tb_user_badge`

사용자가 획득한 뱃지를 기록한다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| user_badge_id | Long (PK) | 사용자 뱃지 ID |
| user_id | Long | 사용자 ID |
| badge_id | Long (FK) | 뱃지 ID (ManyToOne → Badge) |
| acquired_at | LocalDateTime | 획득 일시 |
| pinned | Boolean | 프로필/커리어 고정 여부 (default: false) |
| revoked | Boolean | 회수 여부 (default: false) |
| revoked_at | LocalDateTime | 회수 일시 |
| revoke_reason | String(200) | 회수 사유 |

---

### UserReward (사용자 EXP/레벨)

> 테이블: `tb_user_reward`

사용자별 EXP/레벨 집계 (1 user : 1 row).

| 컬럼 | 타입 | 설명 |
|---|---|---|
| user_reward_id | Long (PK) | ID |
| user_id | Long (UK) | 사용자 ID |
| total_exp | Long | 누적 총 EXP |
| level | Integer | 현재 레벨 (default: 1) |
| current_level_exp | Long | 현재 레벨에서 획득한 EXP (default: 0) |
| next_level_exp | Long | 다음 레벨까지 필요 EXP (default: 100) |
| season_exp | Long | 현재 시즌 EXP |
| season_code | String(20) | 현재 시즌 코드 (예: 2026-Q2) |

---

### UserStreak (연속 인증 스트릭)

> 테이블: `tb_user_streak`

사용자의 연속 인증 글쓰기 기록을 관리한다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| streak_id | Long (PK) | 스트릭 ID |
| user_id | Long (UK) | 사용자 ID |
| current_streak | Integer | 현재 연속 일수 (default: 0) |
| max_streak | Integer | 역대 최대 연속 일수 (default: 0) |
| last_posted_date | LocalDate | 마지막 인증 글 작성일 |
| recovery_tickets | Integer | 복구 티켓 수 (default: 0) |
| last_milestone_reached | Integer | 마지막 달성 마일스톤 (3/7/14/30/100) |

---

### DailyMission (일일 미션 마스터)

> 테이블: `tb_daily_mission`

일일 미션 정의 마스터 테이블.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| mission_id | Long (PK) | 미션 ID |
| code | String(60) | 미션 코드 (UK) |
| title | String(100) | 미션 제목 |
| description | String(300) | 미션 상세 설명 |
| difficulty | Enum | 난이도 (EASY/NORMAL/HARD) |
| target_count | Integer | 완료 목표 횟수 (default: 1) |
| reward_exp | Integer | 완료 시 지급 EXP |
| grants_gacha_ticket | Boolean | Hard 미션 가챠 티켓 지급 여부 (default: false) |
| is_fixed_candidate | Boolean | 매일 고정 후보 여부 (default: false) |
| activated | Boolean | 활성 여부 (default: true) |

---

### UserDailyMissionAssignment (사용자 일일 미션 배정)

> 테이블: `tb_user_daily_mission_assignment`

사용자별 일일 미션 배정 및 진행 상태를 기록한다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| assignment_id | Long (PK) | 배정 ID |
| user_id | Long | 사용자 ID |
| mission_id | Long (FK) | 미션 ID (ManyToOne → DailyMission) |
| assigned_date | LocalDate | 배정 일자 |
| current_count | Integer | 현재 진행 횟수 (default: 0) |
| target_count | Integer | 목표 횟수 (스냅샷) |
| status | Enum | 진행 상태 (default: IN_PROGRESS) |
| completed_at | LocalDateTime | 완료 일시 |
| claimed_at | LocalDateTime | 보상 수령 일시 |

---

### BattlePrediction (배틀 예측)

> 테이블: `tb_battle_prediction`

사용자의 배틀 1등 예측 기록.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| prediction_id | Long (PK) | 예측 ID |
| user_id | Long | 예측 사용자 ID |
| battle_id | Long (FK) | 배틀 ID (ManyToOne → Battle) |
| predicted_item_id | Long (FK) | 사용자가 예측한 1등 아이템 |
| winner_item_id | Long (FK) | 실제 1등 아이템 |
| hit | Boolean | 적중 여부 (배틀 종료 전 null) |
| settled_at | LocalDateTime | 적중 판정 일시 |

---

### CategoryMastery (카테고리 숙련도)

> 테이블: `tb_category_mastery`

사용자의 카테고리별 전문성 성장 단계를 관리한다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| mastery_id | Long (PK) | 숙련도 ID |
| user_id | Long | 사용자 ID |
| category_id | Long (FK) | 카테고리 ID (ManyToOne → Category) |
| tier | Enum | 현재 등급 (NONE/INTERESTED/ENTHUSIAST/CURATOR/EXPERT) |
| feed_count | Integer | 카테고리 인증 피드 수 (default: 0) |
| battle_vote_count | Integer | 배틀 투표 수 (default: 0) |
| pick_comment_count | Integer | PICK 댓글 수 (default: 0) |
| prediction_total_count | Integer | 예측 총 횟수 (default: 0) |
| prediction_hit_count | Integer | 예측 적중 횟수 (default: 0) |
| prediction_accuracy | Decimal(5,4) | 예측 정확도 (0.00 ~ 1.00) |
| monthly_prediction_king_count | Integer | 월간 예측왕 달성 횟수 (default: 0) |

---

## 관계도

```
UserReward (1:1) ── User
UserStreak (1:1) ── User
UserBadge (N:1) ── Badge
UserDailyMissionAssignment (N:1) ── DailyMission
BattlePrediction (N:1) ── Battle, BattleItem
CategoryMastery (N:1) ── Category
```
