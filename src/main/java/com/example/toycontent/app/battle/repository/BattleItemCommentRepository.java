package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.BattleItemComment;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleItemCommentRepository extends JpaRepository<BattleItemComment, Long> {

  Slice<BattleItemComment> findByBattleItemIdAndIsDeletedFalseOrderByLikeCountDesc(Long battleItemId, Pageable pageable);

  Slice<BattleItemComment> findByBattleItemIdAndIsDeletedFalseOrderByCreatedAtDesc(Long battleItemId, Pageable pageable);

  Optional<BattleItemComment> findByIdAndIsDeletedFalse(Long id);
}