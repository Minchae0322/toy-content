package com.example.toycontent.app.notification.outbox;

public enum NotificationOutboxStatus {
  /** 커밋됐고 아직 ack를 못 받았다. 즉시 전송 또는 릴레이가 보낸다. */
  PENDING,
  /** 브로커 ack를 받았다. 정리 배치가 보존 기간 뒤 삭제한다. */
  SENT,
  /** 최대 시도 횟수를 넘겼다. 릴레이가 더 집지 않으며 운영자가 본다. */
  DEAD
}
