package com.example.toycontent.app.reward.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
    name = "tb_user_badge",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_badge_user_badge",
        columnNames = {"user_id", "badge_id"}
    ),
    indexes = {
        @Index(name = "idx_user_badge_user", columnList = "user_id"),
        @Index(name = "idx_user_badge_acquired_at", columnList = "acquired_at DESC")
    }
)
@Comment("유저가 획득한 뱃지")
public class UserBadge extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_badge_id")
  @Comment("유저 뱃지 ID")
  private Long id;

  @Column(name = "user_id", nullable = false)
  @Comment("유저 ID")
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "badge_id", nullable = false)
  @Comment("뱃지")
  private Badge badge;

  @Column(name = "acquired_at", nullable = false)
  @Comment("획득 일시")
  private LocalDateTime acquiredAt;

  @Builder.Default
  @Column(name = "pinned", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
  @Comment("프로필/캐리어 대표 뱃지 고정 여부")
  private Boolean pinned = false;

  @Builder.Default
  @Column(name = "revoked", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
  @Comment("어뷰징 등으로 회수되었는지")
  private Boolean revoked = false;

  @Column(name = "revoked_at")
  @Comment("회수 일시")
  private LocalDateTime revokedAt;

  @Column(name = "revoke_reason", length = 200)
  @Comment("회수 사유")
  private String revokeReason;
}
