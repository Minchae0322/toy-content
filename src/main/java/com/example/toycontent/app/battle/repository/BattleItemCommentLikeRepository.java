package com.example.toycontent.app.battle.repository;


import com.example.toycontent.app.battle.domain.BattleItemCommentLike;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleItemCommentLikeRepository extends JpaRepository<BattleItemCommentLike, Long> {

  Optional<BattleItemCommentLike> findByBattleItemCommentIdAndMemberId(Long battleItemCommentId, Long memberId);

  /** 공감 여부 일괄 조회 (N+1 방지) */
  @Query("SELECT l.battleItemComment.id FROM BattleItemCommentLike l "
      + "WHERE l.battleItemComment.id IN :commentIds AND l.creatorId = :creatorId")
  Set<Long> findBattleItemCommentIdsByCommentIdsAndMemberId(
      @Param("commentIds") List<Long> commentIds,
      @Param("creatorId") Long creatorId);
}
