package com.example.toycontent.app.kafka.dto;


import com.example.toycontent.app.common.enumuration.NotificationChannel;
import com.example.toycontent.app.common.enumuration.NotificationReferenceType;
import com.example.toycontent.app.common.enumuration.NotificationType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaNotificationDto {

  private Long userId;
  private NotificationType type;
  private String title;
  private String content;

  private String referenceId;
  private NotificationReferenceType referenceType;

  /** 클릭 시 이동 경로 (예: /battle/123, /feed/456) */
  private String actionUrl;

  private List<NotificationChannel> channels;

  /** 발신자 정보 */
  private Long actorId;
  private String actorNickname;
  private String actorProfileImageUrl;
}
