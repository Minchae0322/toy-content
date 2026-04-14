package com.example.toycontent.app.reward.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    name = "tb_badge",
    uniqueConstraints = @UniqueConstraint(name = "uk_badge_code", columnNames = "code")
)
@Comment("뱃지 정의 마스터")
public class Badge extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "badge_id")
  @Comment("뱃지 ID")
  private Long id;

  @Column(name = "code", nullable = false, length = 60)
  @Comment("뱃지 코드 (예: BUY_PLACE_SHARER, HOT_MAKER)")
  private String code;

  @Column(name = "name", nullable = false, length = 80)
  @Comment("뱃지 이름 (예: 구매처 쉐어러)")
  private String name;

  @Column(name = "description", length = 300)
  @Comment("뱃지 설명 / 획득 조건 문구")
  private String description;

  @Column(name = "icon_emoji", length = 10)
  @Comment("아이콘 이모지 (예: 🏪)")
  private String iconEmoji;

  @Column(name = "icon_image_url", length = 500)
  @Comment("뱃지 이미지 URL")
  private String iconImageUrl;

  @Column(name = "category", length = 30)
  @Comment("뱃지 카테고리 (BRAG/CURATION/STREAK/SEASON 등)")
  private String category;

  @Builder.Default
  @Column(name = "is_seasonal", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
  @Comment("시즌 한정 뱃지 여부")
  private Boolean isSeasonal = false;

  @Column(name = "season_code", length = 20)
  @Comment("시즌 한정일 경우 시즌 코드")
  private String seasonCode;

  @Builder.Default
  @Column(name = "activated", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
  @Comment("활성 여부")
  private Boolean activated = true;
}
