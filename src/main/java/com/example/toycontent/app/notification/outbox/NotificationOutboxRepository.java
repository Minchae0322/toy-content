package com.example.toycontent.app.notification.outbox;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

  /**
   * 릴레이가 집을 행. 즉시 전송이 아직 진행 중일 수 있는 최근 행은 제외한다 ({@code createdBefore}).
   * created_at 순이라 같은 사용자 알림의 순서가 유지된다.
   */
  @Query("select o from NotificationOutbox o "
      + "where o.status = com.example.toycontent.app.notification.outbox.NotificationOutboxStatus.PENDING "
      + "and o.createdAt < :createdBefore order by o.createdAt asc, o.id asc")
  List<NotificationOutbox> findPendingBatch(LocalDateTime createdBefore, Pageable pageable);

  long countByStatus(NotificationOutboxStatus status);

  @Modifying
  @Query("delete from NotificationOutbox o "
      + "where o.status = com.example.toycontent.app.notification.outbox.NotificationOutboxStatus.SENT "
      + "and o.sentAt < :sentBefore")
  int deleteSentBefore(LocalDateTime sentBefore);
}
