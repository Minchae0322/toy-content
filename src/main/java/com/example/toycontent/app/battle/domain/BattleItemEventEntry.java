package com.example.toycontent.app.battle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * 이벤트 배틀 참여 메타 — 사용자가 아이템 추가 시 입력한 event_id를 저장한다.
 *
 * <p>응답 DTO에 절대 매핑하지 않으며 운영 admin/집계에서만 조회한다.
 * 이벤트 종료 시 테이블을 통째로 drop해 깔끔하게 회수할 수 있도록 본 도메인과 분리한다.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
    name = "TB_BATTLE_ITEM_EVENT_ENTRY",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_battle_item_event_entry_battle_item",
            columnNames = "battle_item_id"
        )
    }
)
public class BattleItemEventEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battle_item_event_entry_id")
  private Long id;

  @Column(name = "battle_item_id", nullable = false)
  @Comment("배틀 아이템 ID")
  private Long battleItemId;

  @Column(name = "event_id", nullable = false, length = 50)
  @Comment("사용자 입력 이벤트 ID")
  private String eventId;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public static BattleItemEventEntry of(Long battleItemId, String eventId) {
    return BattleItemEventEntry.builder()
        .battleItemId(battleItemId)
        .eventId(eventId)
        .build();
  }
}
