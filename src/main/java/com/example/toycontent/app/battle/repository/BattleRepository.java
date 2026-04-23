package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.repository.querydsl.BattleRepositoryCustom;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleRepository extends JpaRepository<Battle, Long>, BattleRepositoryCustom {
  
  long countByCreatorIdAndStatus(Long userId, BattleStatus battleStatus);

  long countByCreatorIdAndCreatedAtAfter(Long userId, LocalDateTime dayAgo);

  @Query("SELECT COUNT(b) FROM Battle b WHERE b.creatorId = :userId AND b.isDeleted = false AND b.endDate > :now")
  long countOngoingByCreatorId(@Param("userId") Long userId, @Param("now") LocalDateTime now);


  @Query("""
    SELECT b FROM Battle b
    JOIN BattleItem bi ON bi.battle.id = b.id
    WHERE bi.product.id = :productId
    AND (:cursor IS NULL OR b.id < :cursor)
    ORDER BY b.id DESC
    LIMIT :limit
    """)
  List<Battle> findBattlesContainingProduct(
      @Param("productId") Long productId,
      @Param("cursor") Long cursor,
      @Param("limit") int limit
  );

  /**
   * 진행중인 배틀 수 조회
   * - 삭제되지 않은 배틀 중 현재 시각이 startDate ~ endDate 사이인 것
   */
  @Query("SELECT COUNT(b) FROM Battle b WHERE b.isDeleted = false AND b.startDate <= :now AND b.endDate > :now")
  long countActiveBattles(@Param("now") LocalDateTime now);
}
