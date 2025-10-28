package com.example.toycontent.app.battle.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.BattleCategory;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.BattleType;
import com.example.toycontent.app.common.enumuration.ResultVisibility;
import com.example.toycontent.app.common.enumuration.VoteType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

  // 기본 정보
  @Column(nullable = false, length = 50)
  @Comment("배틀 제목")
  private String title;

  @Column(length = 500)
  @Comment("배틀 설명")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Comment("카테고리")
  private BattleCategory category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Comment("배틀 타입")
  private BattleType type;

  // 생성자
  @Column(name = "creator_id", nullable = false)
  @Comment("생성자 ID")
  private Long creatorId;

  // 기간 설정
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
  @Comment("결과 공개 시점")
  private ResultVisibility resultVisibility;

  @Builder.Default
  @Column(nullable = false)
  @Comment("중복 제품 허용 여부")
  private Boolean allowDuplicateProducts = false;

  // 리워드 설정
  @Column(length = 200)
  @Comment("추가 리워드")
  private String additionalReward;

  @Builder.Default
  @Column(nullable = false)
  @Comment("추가 리워드 여부")
  private Boolean hasAdditionalReward = false;

  // 상태
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Comment("배틀 상태")
  private BattleStatus status;

  // 통계
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
  private Integer totalViews = 0;

  @Builder.Default
  @Comment("삭제 여부")
  private Boolean isDeleted = false;

  // 연관 관계
  @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BattleItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BattleVote> votes = new ArrayList<>();

  @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BattleParticipation> participations = new ArrayList<>();



}
