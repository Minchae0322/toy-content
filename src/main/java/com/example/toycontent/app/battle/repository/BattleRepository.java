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
}
