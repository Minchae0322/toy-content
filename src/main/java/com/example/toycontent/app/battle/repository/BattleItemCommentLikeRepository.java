package com.example.toycontent.app.battle.repository;


import com.example.toycontent.app.battle.domain.BattleItemCommentLike;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleItemCommentLikeRepository extends JpaRepository<BattleItemCommentLike, Long> {

  Optional<BattleItemCommentLike> findByBattleItemCommentIdAndCreatorId(Long battleItemCommentId, Long creatorId);
}
