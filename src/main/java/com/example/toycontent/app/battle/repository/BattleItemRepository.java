package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.repository.querydsl.BattleItemRepositoryCustom;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleItemRepository extends JpaRepository<BattleItem, Long>,
    BattleItemRepositoryCustom {

  long countByBattleAndIsDeletedFalse(Battle battle);

  @Query("""
    SELECT bi FROM BattleItem bi
    LEFT JOIN FETCH bi.product p
    WHERE bi.battle.id IN :battleIds
    ORDER BY bi.battle.id, bi.totalScore DESC
    """)
  List<BattleItem> findItemsByBattleIds(@Param("battleIds") List<Long> battleIds);

  long countByProductIdAndCreatedAtAfter(Long productId, LocalDateTime recentPeriod);

  @Query("SELECT bi.product.id, COUNT(bi) FROM BattleItem bi WHERE bi.product.id IN :productIds AND bi.createdAt > :since GROUP BY bi.product.id")
  List<Object[]> countByProductIdsAndCreatedAtAfter(@Param("productIds") List<Long> productIds, @Param("since") LocalDateTime since);

}
