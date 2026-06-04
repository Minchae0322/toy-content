package com.example.toycontent.app.battle.controller.dto;

import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse.BattleItemCommentSummary;
import com.example.toycontent.app.battle.controller.dto.BattleVoteResponse.UserBattleVote;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleVote;
import com.example.toycontent.app.category.contoller.dto.CategoryResponse.SubCategoryDetail;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleItemType;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.UserRewardInfo;
import com.example.toycontent.app.reward.exp.service.dto.ExpGrantInfo;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.example.toycontent.app.product.controller.dto.ProductResponse.BattleItemProduct;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class BattleResponse {

  @Schema(description = "배틀 목록 조회 응답")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BattleHotList {
    @Schema(description = "배틀 ID", example = "1")
    private Long id;

    @Schema(description = "배틀 제목", example = "2025년 최고의 스니커즈를 찾아라")
    private String title;

    @Schema(description = "대표 이미지")
    private AttachmentFileResponse thumbnailDto;

    @Schema(description = "상위 아이템 이미지 목록 ((최대 4개) 대표이미지가 없으면)")
    private List<String> topItemImages;

    @Schema(description = "총 참여자 수", example = "48")
    private Integer totalParticipants;

    @Schema(description = "총 투표 수", example = "127")
    private Integer totalVotes;

    @Schema(description = "총 조회 수", example = "523")
    private Integer totalViews;

    @Schema(description = "총 댓글 수", example = "5")
    private Integer totalCommentCount;

    @Schema(description = "배틀 상태", example = "ACTIVE")
    private BattleStatus status;

    @Schema(description = "종료일", example = "2025-02-28T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "TOP 3 아이템 목록")
    private List<BattleHotItem> topItems;

  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "핫 배틀 TOP 3 아이템")
  public static class BattleHotItem {
    @Schema(description = "아이템 ID")
    private Long id;

    @Schema(description = "아이템명")
    private String displayName;

    @Schema(description = "득표 스코어")
    private Integer totalScore;

    @Schema(description = "득표율 (%)", example = "33.07")
    private Double votePercentage;

    @Schema(description = "순위")
    private Integer rank;

    public static BattleHotItem from(BattleItem item, int rank) {
      int battleTotalScore = item.getBattle().getTotalScore();
      return BattleHotItem.builder()
          .id(item.getId())
          .displayName(item.getDisplayName())
          .totalScore(item.getTotalScore())
          .votePercentage(battleTotalScore > 0
              ? (double) item.getTotalScore() / battleTotalScore
              : 0.0)
          .rank(rank)
          .build();
    }
  }

  @Schema(description = "배틀 목록 조회 응답")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BattleList {

    @Schema(description = "배틀 ID", example = "1")
    private Long id;

    @Schema(description = "배틀 제목", example = "2025년 최고의 스니커즈를 찾아라")
    private String title;

    @Schema(description = "카테고리명", example = "스니커즈")
    private SubCategoryDetail subCategoryDetail;

    @Schema(description = "배틀 상태", example = "ACTIVE")
    private BattleStatus status;

    @Schema(description = "생성자 Dto", example = "스니커헤드123")
    private ExternalUserInfo creatorUserInfo;

    @Schema(description = "아이템 추가 권한 타입", example = "CREATOR_ONLY")
    private ItemAddPermissionType itemAddPermissionType;

    @Schema(description = "총 참여자 수", example = "48")
    private Integer totalParticipants;

    @Schema(description = "총 투표 수", example = "127")
    private Integer totalVotes;

    @Schema(description = "총 조회 수", example = "523")
    private Integer totalViews;

    @Schema(description = "총 댓글 수", example = "5")
    private Integer totalCommentCount;

    @Schema(description = "시작일", example = "2025-02-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "종료일", example = "2025-02-28T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "생성일", example = "2025-01-25T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "대표 이미지")
    private AttachmentFileResponse thumbnailDto;

    @Schema(description = "사용자 티어 정보", example = "")
    private UserRewardInfo userRewardInfo;

    @Schema(description = "상위 아이템 이미지 목록 ((최대 4개) 대표이미지가 없으면)")
    private List<String> topItemImages;

    @Schema(hidden = true)
    @JsonIgnore
    private Long creatorId;
  }

  @Schema(description = "배틀 상세 조회 응답")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BattleDetail {

    @Schema(description = "배틀 ID", example = "1")
    private Long id;

    @Schema(description = "배틀 제목", example = "2025년 최고의 스니커즈를 찾아라")
    private String title;

    @Schema(description = "배틀 설명")
    private String description;

    @Schema(description = "카테고리명", example = "스니커즈")
    private SubCategoryDetail subCategoryDetail;

    @Schema(description = "배틀 상태", example = "ACTIVE")
    private BattleStatus status;

    @Schema(description = "생성자 정보", example = "스니커헤드123")
    private ExternalUserInfo creatorUserInfo;

    @NotNull(message = "아이템 추가 권한 타입을 선택해주세요")
    private ItemAddPermissionType itemAddPermissionType;

    @Schema(description = "생성자 프로필 이미지 URL")
    private String creatorProfileImageUrl;

    @Schema(description = "시작일", example = "2025-02-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "종료일", example = "2025-02-28T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "참여 시작일", example = "2025-02-01T00:00:00")
    private LocalDateTime participationStartDate;

    @Schema(description = "투표 타입", example = "SINGLE")
    private VoteType voteType;

    @Schema(description = "총 참여자 수", example = "48")
    private Integer totalParticipants;

    @Schema(description = "총 투표 수", example = "127")
    private Integer totalVotes;

    @Schema(description = "총 조회 수", example = "523")
    private Integer totalViews;

    @Schema(description = "총 댓글 수", example = "5")
    private Integer totalCommentCount;

    @Schema(description = "사용자 티어 정보", example = "")
    private UserRewardInfo userRewardInfo;

    @Schema(description = "생성일", example = "2025-01-25T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "배틀 아이템 목록")
    private List<BattleItemInfo> items;

    public static BattleDetail from(Battle battle, ExternalUserInfo userInfo,
        List<BattleItemInfo> items, UserRewardInfo userRewardInfo) {

      return BattleDetail.builder()
          .id(battle.getId())
          .title(battle.getTitle())
          .description(battle.getDescription())
          .subCategoryDetail(SubCategoryDetail.from(battle.getCategory()))
          .creatorUserInfo(userInfo)
          .itemAddPermissionType(battle.getItemAddPermissionType())
          .status(battle.getStatus())
          .startDate(battle.getStartDate())
          .endDate(battle.getEndDate())
          .participationStartDate(battle.getParticipationStartDate())
          .voteType(battle.getVoteType()).totalParticipants(battle.getTotalParticipants())
          .totalVotes(battle.getTotalVotes())
          .totalViews(battle.getTotalViews())
          .totalCommentCount(battle.getTotalCommentCount())
          .userRewardInfo(userRewardInfo)
          .createdAt(battle.getCreatedAt())
          .items(items)
          .build();
    }

  }


  @Data
  @Schema(description = "배틀 생성 응답")
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BattleCreateResponse {

    @Schema(description = "배틀 ID", example = "1")
    private Long id;

    @Schema(description = "지급된 EXP 정보 (없으면 null)")
    private ExpGrantInfo expGrant;

    public static BattleCreateResponse from(Battle battle, ExpGrantInfo expGrant) {
      return BattleCreateResponse.builder()
          .id(battle.getId())
          .expGrant(expGrant)
          .build();
    }

    public static BattleCreateResponse from(Battle battle) {
      return from(battle, null);
    }
  }

  @Schema(description = "배틀 아이템 정보")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BattleItemInfo implements Rankable {

    @Schema(description = "아이템 ID", example = "1")
    private Long id;

    @Schema(description = "아이템 타입 (PRODUCT: DB 등록 제품, CUSTOM: 사용자 직접입력, YOUTUBE: 유튜브 콘텐츠)",
        example = "PRODUCT")
    private BattleItemType battleItemType;

    @Schema(description = "제품 상세 정보 (itemType=PRODUCT일 때만 존재, 그 외 null)")
    private BattleItemProduct battleItemProduct;

    @Schema(description = "아이템명 - PRODUCT인 경우 null이므로 battleItemProduct.name 사용 필요. "
        + "CUSTOM, YOUTUBE는 이 필드로 노출",
        example = "나이키 덩크 로우 판다")
    private String customName;

    @Schema(description = "브랜드명 - CUSTOM, YOUTUBE 타입에서 사용자가 입력한 값. PRODUCT는 battleItemProduct 쪽 사용",
        example = "Nike")
    private String customBrand;

    @Schema(description = "아이템 이미지 URL - PRODUCT인 경우 null이므로 battleItemProduct.thumbnailDto 사용 필요. "
        + "CUSTOM은 사용자가 등록한 이미지, YOUTUBE는 영상 썸네일")
    private String customImageUrl;

    @Schema(description = "외부 콘텐츠 원본 URL (itemType=YOUTUBE일 때만 존재, 그 외 null). 원본 링크 이동 및 임베드용",
        example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    private String contentUrl;

    @Schema(description = "외부 콘텐츠 임베드 URL (itemType=YOUTUBE일 때만 존재, 그 외 null). iframe src에 바로 사용",
        example = "https://www.youtube.com/embed/dQw4w9WgXcQ")
    private String embedUrl;

    @Schema(description = "투표 수", example = "42")
    private Integer voteCount;

    @Schema(description = "투표 점유율", example = "33.07")
    private Double votePercentage;

    @Schema(description = "득표 스코어", example = "22")
    private Integer totalScore;

    @Schema(description = "랭킹", example = "1")
    private Integer rank;

    @Schema(description = "아이템 상태", example = "ACTIVE")
    private BattleItemStatus status;

    @Schema(description = "신고 수", example = "0")
    private Integer reportCount;

    @Schema(description = "등록자 ID", example = "456")
    private Long registerId;

    @Schema(description = "사용자 투표 정보")
    private UserBattleVote userBattleVote;

    @Schema(description = "BEST 코멘트 및 코멘트 수")
    private BattleItemCommentSummary commentSummary;

    @Schema(description = "스와이프 통계 — VoteType.SWIPE 배틀에만 채워짐 (vote 배틀은 null)")
    private SwipeStats swipeStats;

    @Schema(hidden = true)
    @JsonIgnore
    private Integer rankingScore;

    /**
     * 랭킹 점수 — voteType별 계산식은 {@link VoteType#rankingScoreOf}에 캡슐화되어 있다.
     * 매핑 시점에 미리 계산되어 {@link #rankingScore} 필드에 담기므로 본 메서드는 voteType이 늘어도 변경 불필요.
     */
    @Override
    public Integer getRankingScore() {
      return rankingScore != null ? rankingScore : totalScore;
    }

    public static BattleItemInfo from(BattleItem item,
        BattleItemCommentSummary commentSummary, BattleVote userVote) {

      VoteType voteType = item.getBattle().getVoteType();

      return BattleItemInfo.builder()
          .id(item.getId())
          .battleItemType(item.getItemType())
          .battleItemProduct(productOf(item))
          .customName(item.getDisplayName())
          .customBrand(item.getCustomBrand())
          .customImageUrl(item.getDisplayImageUrl())
          .contentUrl(item.getContentUrl())
          .embedUrl(item.getEmbedUrl())
          .status(item.getStatus())
          .reportCount(item.getReportCount())
          .registerId(item.getRegisterId())
          .voteCount(item.getVoteCount())
          .totalScore(item.getTotalScore())
          .votePercentage(calcVotePercentage(item))
          .userBattleVote(userVote != null ? UserBattleVote.from(userVote) : null)
          .commentSummary(commentSummary)
          .swipeStats(voteType == VoteType.SWIPE ? SwipeStats.from(item) : null)
          .rankingScore(voteType.rankingScoreOf(item))
          .build();
    }

    private static BattleItemProduct productOf(BattleItem item) {
      return item.getProduct() != null ? BattleItemProduct.of(item.getProduct()) : null;
    }

    private static double calcVotePercentage(BattleItem item) {
      int battleTotal = item.getBattle().getTotalScore();
      return battleTotal > 0 ? (double) item.getTotalScore() / battleTotal : 0.0;
    }

  }

  @Schema(description = "스와이프 통계 (VoteType.SWIPE 전용)")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SwipeStats {

    @Schema(description = "강추 PICK 수", example = "12")
    private Integer strongPickCount;

    @Schema(description = "PICK 수", example = "5")
    private Integer pickCount;

    @Schema(description = "PASS 수", example = "3")
    private Integer passCount;

    @Schema(description = "랭킹 점수 (strong*3 + pick*1)", example = "41")
    private Integer score;

    public static SwipeStats from(BattleItem item) {
      return SwipeStats.builder()
          .strongPickCount(item.getStrongPickCount())
          .pickCount(item.getPickCount())
          .passCount(item.getPassCount())
          .score(item.getSwipeRankingScore())
          .build();
    }
  }
}

