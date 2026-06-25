package com.example.toycontent.app.notification;

import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;

/**
 * 알림 발행 요청을 트랜잭션 커밋 시점까지 미루기 위한 인-프로세스 이벤트.
 *
 * <p>서비스 계층은 트랜잭션 안에서 이 이벤트만 publish 하고, 실제 Kafka 발행은
 * {@link NotificationEventListener}가 커밋 이후(AFTER_COMMIT)에 수행한다.
 * 덕분에 롤백된 작업에 대한 "유령 알림"이 구조적으로 차단된다.
 */
public record NotificationEvent(KafkaNotificationDto payload) {
}
