package com.example.toycontent.app.notification;

import com.example.toycontent.app.common.enumuration.NotificationChannel;
import com.example.toycontent.app.common.enumuration.NotificationReferenceType;
import com.example.toycontent.app.common.enumuration.NotificationType;
import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;
import com.example.toycontent.app.notification.outbox.NotificationOutboxStore;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final ApplicationEventPublisher eventPublisher;
  private final NotificationOutboxStore outboxStore;

  // ============================
  // 피드
  // ============================

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

  /**
   * 1위 아이템명을 포함한 종료 알림. 1위가 명확한 배틀(예: SWIPE)에서 사용.
   * winnerName이 null/blank면 호출자가 {@link #notifyBattleResult}로 폴백해야 한다.
   */
  public void notifyBattleResultWithWinner(Long userId, Long battleId, String battleTitle,
      String winnerName) {
    sendSafely(NotificationType.BATTLE_RESULT_WITH_WINNER, KafkaNotificationDto.builder()
        .userId(userId)
        .type(NotificationType.BATTLE_RESULT_WITH_WINNER)
        .title(NotificationType.BATTLE_RESULT_WITH_WINNER.getTitle())
        .content(NotificationType.BATTLE_RESULT_WITH_WINNER.formatContent(battleTitle, winnerName))
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
  // 공통 발행
  // ============================

  /**
   * 실제 Kafka 발행을 직접 호출하지 않고 이벤트만 등록한다. 발행 진입점은 이 메서드 하나다.
   *
   * <p>이 메서드는 호출자의 트랜잭션 스레드에서 동기로 실행되므로, 트랜잭션이 살아 있으면
   * {@link NotificationEventListener}가 커밋 이후(AFTER_COMMIT)에 발행을 수행한다.
   * 롤백 시 이벤트는 폐기되어 유령 알림이 생기지 않는다.
   *
   * <p>등급이 {@code GUARANTEED}면 같은 트랜잭션에 outbox 행을 먼저 남기고 그 id를 이벤트에 싣는다.
   * 롤백이면 행도 함께 사라지고, 커밋이면 리스너가 ack를 받아 SENT로 바꾼다. 리스너가 실패하거나
   * 파드가 죽어 PENDING으로 남은 행은 릴레이가 다시 보낸다. 호출자는 등급을 모른다.
   */
  private void sendSafely(NotificationType type, KafkaNotificationDto dto) {
    Long outboxId = type.isGuaranteed() ? outboxStore.enqueue(type, dto) : null;
    eventPublisher.publishEvent(new NotificationEvent(dto, outboxId));
  }
}
