package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.BattleItemComment;
import com.example.toycontent.app.battle.repository.querydsl.BattleItemCommentRepositoryCustom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleItemCommentRepository extends JpaRepository<BattleItemComment, Long>,
    BattleItemCommentRepositoryCustom {

  @Query(value = """
    SELECT * FROM (
        SELECT bic.battle_item_id,
            bic.battle_item_comment_id,
            bic.creator_nickname,
            bic.content,
            bic.like_count,
            COUNT(*) OVER(PARTITION BY bic.battle_item_id) AS comment_count,
            ROW_NUMBER() OVER(PARTITION BY bic.battle_item_id ORDER BY bic.like_count DESC, bic.created_at ASC) AS rn
        FROM TB_BATTLE_ITEM_COMMENT bic
        WHERE bic.battle_item_id IN :itemIds AND bic.is_deleted = false
    ) t WHERE t.rn = 1
    """, nativeQuery = true)
  List<Object[]> findBestCommentsAndCountByItemIds(@Param("itemIds") List<Long> itemIds);
}