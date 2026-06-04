package com.example.toycontent.app.battle.domain;

import com.example.toycontent.app.common.enumuration.SwipeVerdict;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * 스와이프 배틀의 1회 평가 기록.
 *
 * <p>한 voter가 한 아이템에 대해 단 1회만 스와이프 가능 (변경 불가, 재진입 시 안 보임).
 * voter는 user_id 또는 guest_id 중 한 쪽만 채워진다.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
    name = "TB_BATTLE_SWIPE",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_battle_swipe_user",
            columnNames = {"battle_id", "battle_item_id", "user_id"}
        ),
        @UniqueConstraint(
            name = "uk_battle_swipe_guest",
            columnNames = {"battle_id", "battle_item_id", "guest_id"}
        )
    },
    indexes = {
        @Index(name = "idx_battle_swipe_battle_user", columnList = "battle_id, user_id"),
        @Index(name = "idx_battle_swipe_battle_guest", columnList = "battle_id, guest_id")
    }
)
public class BattleSwipe {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battle_swipe_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "battle_id", nullable = false)
  @Comment("배틀")
  private Battle battle;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "battle_item_id", nullable = false)
  @Comment("배틀 아이템")
  private BattleItem battleItem;

  @Column(name = "user_id")
  @Comment("로그인 사용자 ID (게스트면 null)")
  private Long userId;

  @Column(name = "guest_id", length = 64)
  @Comment("게스트 ID (로그인이면 null)")
  private String guestId;

  @Enumerated(EnumType.STRING)
  @Column(name = "verdict", nullable = false, length = 20)
  @Comment("평가 결과")
  private SwipeVerdict verdict;

  @CreatedDate
  @Column(name = "swiped_at", nullable = false, updatable = false)
  private LocalDateTime swipedAt;

  public static BattleSwipe ofUser(Battle battle, BattleItem item, Long userId,
      SwipeVerdict verdict) {
    return BattleSwipe.builder()
        .battle(battle)
        .battleItem(item)
        .userId(userId)
        .verdict(verdict)
        .build();
  }

  public static BattleSwipe ofGuest(Battle battle, BattleItem item, String guestId,
      SwipeVerdict verdict) {
    return BattleSwipe.builder()
        .battle(battle)
        .battleItem(item)
        .guestId(guestId)
        .verdict(verdict)
        .build();
  }

  /** verdict 갱신. 카운터 동기화는 호출자 책임(서비스에서 -1/+1). */
  public void changeVerdict(SwipeVerdict newVerdict) {
    this.verdict = newVerdict;
  }
}
