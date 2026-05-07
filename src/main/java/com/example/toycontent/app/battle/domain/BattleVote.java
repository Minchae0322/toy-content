package com.example.toycontent.app.battle.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
    name = "TB_BATTLE_VOTE",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_battle_user_rank",
            columnNames = {"battle_id", "user_id", "vote_rank"}
        ),
        @UniqueConstraint(
            name = "uk_battle_guest_rank",
            columnNames = {"battle_id", "guest_id", "vote_rank"}
        )
    }
)
public class BattleVote extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battle_vote_id")
  @Comment("배틀 투표 ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "battle_id", nullable = false)
  @Comment("배틀")
  private Battle battle;

  @Column(name = "user_id")
  @Comment("투표 사용자 (로그인 시)")
  private Long userId;

  @Column(name = "guest_id", length = 36)
  @Comment("게스트 식별자 (비로그인 시 쿠키 기반 UUID)")
  private String guestId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "battle_item_id", nullable = false)
  @Comment("투표 대상 아이템")
  private BattleItem battleItem;

  @Builder.Default
  @Column(nullable = false)
  @Comment("순위")
  private Integer voteRank = 1;

  @Builder.Default
  @Column(nullable = false)
  @Comment("점수")
  private Integer score = 1;

}