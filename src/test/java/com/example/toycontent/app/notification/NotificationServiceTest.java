package com.example.toycontent.app.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.common.enumuration.NotificationChannel;
import com.example.toycontent.app.common.enumuration.NotificationType;
import com.example.toycontent.app.kafka.KafkaNotificationProducer;
import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

  private static final long FEED_OWNER_ID = 100L;
  private static final long ACTOR_ID = 200L;
  private static final String ACTOR_NICKNAME = "테스트유저";
  private static final String ACTOR_PROFILE_URL = "https://example.com/profile.png";
  private static final long FEED_ID = 1L;
  private static final String FEED_TITLE = "피드 제목";

  @Mock private KafkaNotificationProducer kafkaNotificationProducer;

  @InjectMocks private NotificationService notificationService;

  @Nested
  @DisplayName("notifyFeedLike - 피드 좋아요 알림")
  class NotifyFeedLike {

    @Test
    @DisplayName("타인이 내 피드에 좋아요를 눌렀을 때 알림이 발행된다")
    void 타인_좋아요_알림_발행() {
      // when
      notificationService.notifyFeedLike(
          FEED_OWNER_ID, ACTOR_ID, ACTOR_NICKNAME, ACTOR_PROFILE_URL, FEED_ID, FEED_TITLE);

      // then
      ArgumentCaptor<KafkaNotificationDto> captor = ArgumentCaptor.forClass(KafkaNotificationDto.class);
      then(kafkaNotificationProducer).should().send(captor.capture());

      KafkaNotificationDto sent = captor.getValue();
      assertSoftly(softly -> {
        softly.assertThat(sent.getUserId())
            .as("수신 대상은 피드 작성자")
            .isEqualTo(FEED_OWNER_ID);
        softly.assertThat(sent.getType())
            .as("알림 타입")
            .isEqualTo(NotificationType.FEED_LIKE);
        softly.assertThat(sent.getActorId())
            .as("행위자 ID")
            .isEqualTo(ACTOR_ID);
        softly.assertThat(sent.getActorNickname()).isEqualTo(ACTOR_NICKNAME);
        softly.assertThat(sent.getReferenceId()).isEqualTo(String.valueOf(FEED_ID));
        softly.assertThat(sent.getActionUrl()).isEqualTo("/feed/" + FEED_ID);
      });
    }

    @Test
    @DisplayName("자기 자신의 피드에 좋아요를 눌렀을 때는 알림을 발행하지 않는다")
    void 본인_좋아요_알림_제외() {
      // when
      notificationService.notifyFeedLike(
          FEED_OWNER_ID, FEED_OWNER_ID, ACTOR_NICKNAME, ACTOR_PROFILE_URL, FEED_ID, FEED_TITLE);

      // then
      then(kafkaNotificationProducer).should(never()).send(any());
    }

    @Test
    @DisplayName("Kafka producer가 예외를 던져도 메서드는 정상 반환된다")
    void producer_예외_삼킴() {
      // given
      willThrow(new RuntimeException("Kafka down"))
          .given(kafkaNotificationProducer).send(any());

      // when & then - 예외가 밖으로 전파되지 않아야 함
      notificationService.notifyFeedLike(
          FEED_OWNER_ID, ACTOR_ID, ACTOR_NICKNAME, ACTOR_PROFILE_URL, FEED_ID, FEED_TITLE);

      then(kafkaNotificationProducer).should().send(any());
    }
  }

  @Nested
  @DisplayName("notifyFeedComment - 피드 댓글 알림")
  class NotifyFeedComment {

    @Test
    @DisplayName("타인 댓글일 때 FEED_COMMENT 타입 알림이 발행된다")
    void 타인_댓글_알림() {
      // when
      notificationService.notifyFeedComment(
          FEED_OWNER_ID, ACTOR_ID, ACTOR_NICKNAME, ACTOR_PROFILE_URL, FEED_ID, FEED_TITLE);

      // then
      ArgumentCaptor<KafkaNotificationDto> captor = ArgumentCaptor.forClass(KafkaNotificationDto.class);
      then(kafkaNotificationProducer).should().send(captor.capture());
      assertThat(captor.getValue().getType()).isEqualTo(NotificationType.FEED_COMMENT);
    }

    @Test
    @DisplayName("본인 댓글일 때는 알림을 발행하지 않는다")
    void 본인_댓글_알림_제외() {
      // when
      notificationService.notifyFeedComment(
          FEED_OWNER_ID, FEED_OWNER_ID, ACTOR_NICKNAME, ACTOR_PROFILE_URL, FEED_ID, FEED_TITLE);

      // then
      then(kafkaNotificationProducer).should(never()).send(any());
    }
  }

  @Nested
  @DisplayName("notifyBattleItemComment - 배틀 아이템 댓글 알림")
  class NotifyBattleItemComment {

    private static final long BATTLE_ID = 10L;
    private static final long ITEM_ID = 20L;
    private static final long ITEM_CREATOR_ID = 300L;

    @Test
    @DisplayName("타인이 내 배틀 아이템에 댓글을 달면 알림이 발행된다")
    void 타인_배틀_댓글_알림() {
      // when
      notificationService.notifyBattleItemComment(
          ITEM_CREATOR_ID, ACTOR_ID, ACTOR_NICKNAME, ACTOR_PROFILE_URL,
          BATTLE_ID, "배틀 제목", ITEM_ID, "아이템 제목");

      // then
      ArgumentCaptor<KafkaNotificationDto> captor = ArgumentCaptor.forClass(KafkaNotificationDto.class);
      then(kafkaNotificationProducer).should().send(captor.capture());
      assertSoftly(softly -> {
        softly.assertThat(captor.getValue().getUserId()).isEqualTo(ITEM_CREATOR_ID);
        softly.assertThat(captor.getValue().getType()).isEqualTo(NotificationType.BATTLE_ITEM_COMMENT);
        softly.assertThat(captor.getValue().getActionUrl()).isEqualTo("/battles/" + BATTLE_ID);
      });
    }

    @Test
    @DisplayName("본인 배틀 아이템에 스스로 댓글을 달면 알림을 발행하지 않는다")
    void 본인_배틀_댓글_제외() {
      // when
      notificationService.notifyBattleItemComment(
          ITEM_CREATOR_ID, ITEM_CREATOR_ID, ACTOR_NICKNAME, ACTOR_PROFILE_URL,
          BATTLE_ID, "배틀 제목", ITEM_ID, "아이템 제목");

      // then
      then(kafkaNotificationProducer).should(never()).send(any());
    }
  }

  @Nested
  @DisplayName("notifyBattleInvite / notifyBattleResult / notifyFollow / notifySystem - 자기 제외 검사가 없는 알림")
  class OtherNotifications {

    @Test
    @DisplayName("notifyBattleInvite는 초대 대상에게 알림을 발행한다")
    void 배틀_초대_알림() {
      // when
      notificationService.notifyBattleInvite(
          999L, ACTOR_ID, ACTOR_NICKNAME, ACTOR_PROFILE_URL, 10L, "배틀 제목");

      // then
      ArgumentCaptor<KafkaNotificationDto> captor = ArgumentCaptor.forClass(KafkaNotificationDto.class);
      then(kafkaNotificationProducer).should().send(captor.capture());
      assertSoftly(softly -> {
        softly.assertThat(captor.getValue().getUserId()).isEqualTo(999L);
        softly.assertThat(captor.getValue().getType()).isEqualTo(NotificationType.BATTLE_INVITE);
      });
    }

    @Test
    @DisplayName("notifyFollow는 팔로우 대상에게 알림을 발행한다")
    void 팔로우_알림() {
      // when
      notificationService.notifyFollow(999L, ACTOR_ID, ACTOR_NICKNAME, ACTOR_PROFILE_URL);

      // then
      ArgumentCaptor<KafkaNotificationDto> captor = ArgumentCaptor.forClass(KafkaNotificationDto.class);
      then(kafkaNotificationProducer).should().send(captor.capture());
      assertThat(captor.getValue().getType()).isEqualTo(NotificationType.FOLLOW);
    }

    @Test
    @DisplayName("notifySystem은 시스템 알림을 해당 유저에게 발행한다")
    void 시스템_알림() {
      // when
      notificationService.notifySystem(999L, "점검 안내", "/notice/1");

      // then
      ArgumentCaptor<KafkaNotificationDto> captor = ArgumentCaptor.forClass(KafkaNotificationDto.class);
      then(kafkaNotificationProducer).should().send(captor.capture());
      assertSoftly(softly -> {
        softly.assertThat(captor.getValue().getType()).isEqualTo(NotificationType.SYSTEM);
        softly.assertThat(captor.getValue().getActionUrl()).isEqualTo("/notice/1");
      });
    }
  }

  @Nested
  @DisplayName("notifyBattleDeadlineOwnerD7 - 배틀 D-7 생성자 알림")
  class NotifyBattleDeadlineOwnerD7 {

    private static final long CREATOR_ID = 500L;
    private static final long BATTLE_ID = 50L;
    private static final String BATTLE_TITLE = "곧 끝나는 배틀";

    @Test
    @DisplayName("생성자에게 BATTLE_DEADLINE_OWNER_D7 타입 알림이 in-app 채널로만 발행된다")
    void D7_알림_발행() {
      // when
      notificationService.notifyBattleDeadlineOwnerD7(CREATOR_ID, BATTLE_ID, BATTLE_TITLE);

      // then
      ArgumentCaptor<KafkaNotificationDto> captor = ArgumentCaptor.forClass(KafkaNotificationDto.class);
      then(kafkaNotificationProducer).should().send(captor.capture());

      KafkaNotificationDto sent = captor.getValue();
      assertSoftly(softly -> {
        softly.assertThat(sent.getUserId()).isEqualTo(CREATOR_ID);
        softly.assertThat(sent.getType()).isEqualTo(NotificationType.BATTLE_DEADLINE_OWNER_D7);
        softly.assertThat(sent.getReferenceId()).isEqualTo(String.valueOf(BATTLE_ID));
        softly.assertThat(sent.getActionUrl()).isEqualTo("/battles/" + BATTLE_ID);
        softly.assertThat(sent.getContent()).contains(BATTLE_TITLE);
        softly.assertThat(sent.getChannels())
            .as("D-7은 in-app 채널만 사용")
            .containsExactly(NotificationChannel.IN_APP);
      });
    }
  }
}
