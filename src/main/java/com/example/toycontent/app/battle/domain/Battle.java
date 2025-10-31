package com.example.toycontent.app.battle.domain;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.ResultVisibility;
import com.example.toycontent.app.common.enumuration.VoteType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.envers.NotAudited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "TB_BATTLE")
public class Battle extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battle_id")
  @Comment("배틀 ID")
  private Long id;

  @Column(nullable = false, length = 50)
  @Comment("배틀 제목")
  private String title;

  @Column(length = 500)
  @Comment("배틀 설명")
  private String description;

  @JoinColumn(name = "category_id")
  @ManyToOne(fetch = FetchType.LAZY)
  @Comment("카테고리")
  private Category category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  @Comment("아이템 추가 권한 타입")
  private ItemAddPermissionType itemAddPermissionType;

  @Column(name = "creator_id", nullable = false)
  @Comment("생성자 ID")
  private Long creatorId;

  @Column(nullable = false)
  @Comment("시작일")
  private LocalDateTime startDate;

  @Column(nullable = false)
  @Comment("종료일")
  private LocalDateTime endDate;

  @Column(nullable = false)
  @Comment("참여 시작일")
  private LocalDateTime participationStartDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Comment("투표 타입")
  private VoteType voteType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Comment("배틀 상태")
  private BattleStatus status;

  @Builder.Default
  @Column(nullable = false)
  @Comment("총 참여자 수")
  private Integer totalParticipants = 0;

  @Builder.Default
  @Column(nullable = false)
  @Comment("총 투표 수")
  private Integer totalVotes = 0;

  @Builder.Default
  @Column(nullable = false)
  @Comment("총 조회 수")
  @NotAudited
  private Integer totalViews = 0;

  @Builder.Default
  @Comment("삭제 여부")
  private Boolean isDeleted = false;

  @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BattleItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BattleVote> votes = new ArrayList<>();

  @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BattleParticipation> battleParticipationsList = new ArrayList<>();

  @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BattleAttachmentFile> battleAttachmentFiles = new ArrayList<>();


  public void incrementTotalVotes(int delta) {
    this.totalVotes += delta;
  }

  public void incrementTotalParticipants(int delta) {
    this.totalParticipants += delta;
  }

  public void incrementTotalViews() {
    this.totalViews++;

  }



}
