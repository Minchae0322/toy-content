-- =====================================================================
-- TB_NOTIFICATION_OUTBOX 신설 (스키마 변경 이력)
--
-- [적용 방법]
-- 본 프로젝트는 ddl-auto=update 로 동작하므로 아래 변경은
-- 애플리케이션 재기동 시 Hibernate 가 자동 반영한다.
-- 이 스크립트는 스키마 변경 이력 추적 용도로만 보관한다.
--
-- 변경 내용:
-- 1) GUARANTEED 등급 알림(배틀 결과 · D-7 · 승인 요청 · 초대 · 시스템)의 Transactional Outbox
-- 2) 릴레이 조회용 (status, created_at) 인덱스
-- 판단 과정: docs/notifications/알림개선.md
-- =====================================================================

CREATE TABLE TB_NOTIFICATION_OUTBOX (
  notification_outbox_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
  notification_type      VARCHAR(50)  NOT NULL COMMENT '알림 타입',
  user_id                BIGINT       NOT NULL COMMENT '수신자 ID (Kafka 파티션 키)',
  payload                TEXT         NOT NULL COMMENT 'KafkaNotificationDto JSON',
  status                 VARCHAR(10)  NOT NULL COMMENT 'PENDING / SENT / DEAD',
  attempt_count          INT          NOT NULL DEFAULT 0 COMMENT '전송 시도 횟수 (즉시 전송 포함)',
  last_error             VARCHAR(500) NULL COMMENT '마지막 실패 사유',
  created_at             DATETIME(6)  NOT NULL,
  sent_at                DATETIME(6)  NULL,
  PRIMARY KEY (notification_outbox_id),
  INDEX idx_notification_outbox_status_created (status, created_at)
);
