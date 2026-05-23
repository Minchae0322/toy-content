package com.example.toycontent.app.notification.hotcontent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 핫 콘텐츠 발견 알림 발송 이력. {@code (content_type, content_id)} 단위 영구 dedup —
 * 같은 콘텐츠는 다시 후보로 뽑혀도 한 번만 브로드캐스트한다.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
    name = "TB_HOT_CONTENT_NOTIFICATION_SENT",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_hot_content_notification_sent",
            columnNames = {"content_type", "content_id"}
        )
    },
    indexes = {
        @Index(name = "idx_hot_content_notification_sent_type", columnList = "content_type")
    }
)
public class HotContentNotificationSent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "hot_content_notification_sent_id")
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "content_type", nullable = false, length = 20)
  @Comment("콘텐츠 종류 (FEED, BATTLE)")
  private HotContentType contentType;

  @Column(name = "content_id", nullable = false)
  @Comment("콘텐츠 ID")
  private Long contentId;

  @CreatedDate
  @Column(name = "sent_at", nullable = false, updatable = false)
  private LocalDateTime sentAt;

  public static HotContentNotificationSent of(HotContentType contentType, Long contentId) {
    return HotContentNotificationSent.builder()
        .contentType(contentType)
        .contentId(contentId)
        .build();
  }
}
