package com.example.toycontent.app.notification;

import com.example.toycontent.app.common.enumuration.NotificationChannel;
import com.example.toycontent.app.common.enumuration.NotificationReferenceType;
import com.example.toycontent.app.common.enumuration.NotificationType;
import com.example.toycontent.app.kafka.KafkaNotificationProducer;
import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final KafkaNotificationProducer notificationProducer;

  // ============================
  // 피드
  // ============================

  @Async("notificationExecutor")
  public void notifyFeedComment(Long feedCreatorId, Long actorId, String actorNickname,
      String actorProfileImageUrl, Long feedId, String feedTitle) {
    if(feedCreatorId.equals(actorId)) {
      return;
    }

    sendSafely(NotificationType.FEED_COMMENT, KafkaNotificationDto.builder()
        .userId(feedCreatorId)
        .type(NotificationType.FEED_COMMENT)
        .title(NotificationType.FEED_COMMENT.getTitle())
        .content(NotificationType.FEED_COMMENT.formatContent(actorNickname, feedTitle))
        .referenceId(String.valueOf(feedId))
        .referenceType(NotificationReferenceType.FEED)
        .actionUrl("/feed/" + feedId)
        .actorId(actorId)
        .actorNickname(actorNickname)
        .actorProfileImageUrl(actorProfileImageUrl)
        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
        .build()
    );
  }

  @Async("notificationExecutor")
  public void notifyFeedLike(Long feedCreatorId, Long actorId, String actorNickname,
      String actorProfileImageUrl, Long feedId, String feedTitle) {
    if(feedCreatorId.equals(actorId)) {
      return;
    }

    sendSafely(NotificationType.FEED_LIKE, KafkaNotificationDto.builder()
        .userId(feedCreatorId)
        .type(NotificationType.FEED_LIKE)
        .title(NotificationType.FEED_LIKE.getTitle())
        .content(NotificationType.FEED_LIKE.formatContent(actorNickname, feedTitle))
        .referenceId(String.valueOf(feedId))
        .referenceType(NotificationReferenceType.FEED)
        .actionUrl("/feed/" + feedId)
        .actorId(actorId)
        .actorNickname(actorNickname)
        .actorProfileImageUrl(actorProfileImageUrl)
        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
        .build()
    );
  }

  // ============================
  // 배틀 아이템
  // ============================

  @Async("notificationExecutor")
  public void notifyBattleItemComment(Long battleItemCreatorId, Long actorId, String actorNickname,
      String actorProfileImageUrl,
      Long battleId, String battleTitle,
      Long itemId, String itemTitle) {
    if(battleItemCreatorId.equals(actorId)) {
      return;
    }

    sendSafely(NotificationType.BATTLE_ITEM_COMMENT, KafkaNotificationDto.builder()
        .userId(battleItemCreatorId)
        .type(NotificationType.BATTLE_ITEM_COMMENT)
        .title(NotificationType.BATTLE_ITEM_COMMENT.getTitle())
        .content(NotificationType.BATTLE_ITEM_COMMENT.formatContent(actorNickname, battleTitle, itemTitle))
        .referenceId(String.valueOf(itemId))
        .referenceType(NotificationReferenceType.BATTLE_ITEM)
        .actionUrl("/battles/" + battleId)
        .actorId(actorId)
        .actorNickname(actorNickname)
        .actorProfileImageUrl(actorProfileImageUrl)
        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
        .build()
    );
  }

  @Async("notificationExecutor")
  public void notifyBattleItemAdded(Long battleCreatorId, Long actorId, String actorNickname,
      String actorProfileImageUrl,
      Long battleId, String battleTitle,
      Long firstItemId, String firstItemTitle, int additionalCount) {
    if (battleCreatorId.equals(actorId)) {
      return;
    }

    boolean batch = additionalCount > 0;
    NotificationType type = batch ? NotificationType.BATTLE_ITEM_ADDED_BATCH : NotificationType.BATTLE_ITEM_ADDED;
    String content = batch
        ? type.formatContent(actorNickname, battleTitle, firstItemTitle, String.valueOf(additionalCount))
        : type.formatContent(actorNickname, battleTitle, firstItemTitle);

    sendSafely(type, KafkaNotificationDto.builder()
        .userId(battleCreatorId)
        .type(type)
        .title(type.getTitle())
        .content(content)
        .referenceId(String.valueOf(firstItemId))
        .referenceType(NotificationReferenceType.BATTLE_ITEM)
        .actionUrl("/battles/" + battleId)
        .actorId(actorId)
        .actorNickname(actorNickname)
        .actorProfileImageUrl(actorProfileImageUrl)
        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
        .build()
    );
  }

  @Async("notificationExecutor")
  public void notifyBattleItemApprovalRequest(Long battleCreatorId, Long actorId, String actorNickname,
      String actorProfileImageUrl,
      Long battleId, String battleTitle,
      Long firstItemId, String firstItemTitle, int additionalCount) {
    if (battleCreatorId.equals(actorId)) {
      return;
    }

    boolean batch = additionalCount > 0;
    NotificationType type = batch ? NotificationType.BATTLE_ITEM_APPROVAL_REQUEST_BATCH : NotificationType.BATTLE_ITEM_APPROVAL_REQUEST;
    String content = batch
        ? type.formatContent(actorNickname, battleTitle, firstItemTitle, String.valueOf(additionalCount))
        : type.formatContent(actorNickname, battleTitle, firstItemTitle);

    sendSafely(type, KafkaNotificationDto.builder()
        .userId(battleCreatorId)
        .type(type)
        .title(type.getTitle())
        .content(content)
        .referenceId(String.valueOf(firstItemId))
        .referenceType(NotificationReferenceType.BATTLE_ITEM)
        .actionUrl("/battles/" + battleId)
        .actorId(actorId)
        .actorNickname(actorNickname)
        .actorProfileImageUrl(actorProfileImageUrl)
        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
        .build()
    );
  }

  @Deprecated
  @Async("notificationExecutor")
  public void notifyBattleItemLike(Long battleItemCreatorId, Long actorId, String actorNickname,
      String actorProfileImageUrl,
      Long battleId, String battleTitle,
      Long itemId, String itemTitle) {
    if(battleItemCreatorId.equals(actorId)) {
      return;
    }

    sendSafely(NotificationType.BATTLE_ITEM_LIKE, KafkaNotificationDto.builder()
        .userId(battleItemCreatorId)
        .type(NotificationType.BATTLE_ITEM_LIKE)
        .title(NotificationType.BATTLE_ITEM_LIKE.getTitle())
        .content(NotificationType.BATTLE_ITEM_LIKE.formatContent(actorNickname, battleTitle, itemTitle))
        .referenceId(String.valueOf(itemId))
        .referenceType(NotificationReferenceType.BATTLE_ITEM)
        .actionUrl("/battles/" + battleId + "/item/" + itemId)
        .actorId(actorId)
        .actorNickname(actorNickname)
        .actorProfileImageUrl(actorProfileImageUrl)
        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
        .build()
    );
  }

  // ============================
  // 배틀
  // ============================

  @Async("notificationExecutor")
  public void notifyBattleInvite(Long targetUserId, Long actorId, String actorNickname,
      String actorProfileImageUrl,
      Long battleId, String battleTitle) {
    sendSafely(NotificationType.BATTLE_INVITE, KafkaNotificationDto.builder()
        .userId(targetUserId)
        .type(NotificationType.BATTLE_INVITE)
        .title(NotificationType.BATTLE_INVITE.getTitle())
        .content(NotificationType.BATTLE_INVITE.formatContent(actorNickname, battleTitle))
        .referenceId(String.valueOf(battleId))
        .referenceType(NotificationReferenceType.BATTLE)
        .actionUrl("/battles/" + battleId)
        .actorId(actorId)
        .actorNickname(actorNickname)
        .actorProfileImageUrl(actorProfileImageUrl)
        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
        .build()
    );
  }

  @Async("notificationExecutor")
  public void notifyBattleDeadlineOwnerD7(Long creatorId, Long battleId, String battleTitle) {
    sendSafely(NotificationType.BATTLE_DEADLINE_OWNER_D7, KafkaNotificationDto.builder()
        .userId(creatorId)
        .type(NotificationType.BATTLE_DEADLINE_OWNER_D7)
        .title(NotificationType.BATTLE_DEADLINE_OWNER_D7.getTitle())
        .content(NotificationType.BATTLE_DEADLINE_OWNER_D7.formatContent(battleTitle))
        .referenceId(String.valueOf(battleId))
        .referenceType(NotificationReferenceType.BATTLE)
        .actionUrl("/battles/" + battleId)
        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
        .build()
    );
  }

  @Async("notificationExecutor")
  public void notifyBattleResult(Long userId, Long battleId, String battleTitle) {
    sendSafely(NotificationType.BATTLE_RESULT, KafkaNotificationDto.builder()
        .userId(userId)
        .type(NotificationType.BATTLE_RESULT)
        .title(NotificationType.BATTLE_RESULT.getTitle())
        .content(NotificationType.BATTLE_RESULT.formatContent(battleTitle))
        .referenceId(String.valueOf(battleId))
        .referenceType(NotificationReferenceType.BATTLE)
        .actionUrl("/battles/" + battleId)
        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
        .build()
    );
  }

  // ============================
  // 소셜
  // ============================

  @Async("notificationExecutor")
  public void notifyFollow(Long targetUserId, Long actorId, String actorNickname,
      String actorProfileImageUrl) {
    sendSafely(NotificationType.FOLLOW, KafkaNotificationDto.builder()
        .userId(targetUserId)
        .type(NotificationType.FOLLOW)
        .title(NotificationType.FOLLOW.getTitle())
        .content(NotificationType.FOLLOW.formatContent(actorNickname))
        .referenceId(String.valueOf(actorId))
        .referenceType(NotificationReferenceType.USER)
        .actionUrl("/profile/" + actorId)
        .actorId(actorId)
        .actorNickname(actorNickname)
        .actorProfileImageUrl(actorProfileImageUrl)
        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
        .build()
    );
  }

  // ============================
  // 시스템
  // ============================

  @Async("notificationExecutor")
  public void notifySystem(Long userId, String content, String actionUrl) {
    sendSafely(NotificationType.SYSTEM, KafkaNotificationDto.builder()
        .userId(userId)
        .type(NotificationType.SYSTEM)
        .title(NotificationType.SYSTEM.getTitle())
        .content(NotificationType.SYSTEM.formatContent(content))
        .referenceType(NotificationReferenceType.SYSTEM)
        .actionUrl(actionUrl)
        .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
        .build()
    );
  }

  // ============================
  // 공통 안전 발행
  // ============================

  private void sendSafely(NotificationType type, KafkaNotificationDto dto) {
    try {
      notificationProducer.send(dto);
    } catch (Exception e) {
      log.error("[Notification] 알림 발행 실패: userId={}, type={}, error={}",
          dto.getUserId(), type, e.getMessage(), e);
    }
  }
}