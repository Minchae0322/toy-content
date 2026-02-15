package com.example.toycontent.app.battle.repository.querydsl;

import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse;
import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse.Detail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface BattleItemCommentRepositoryCustom {
  Slice<Detail> findComments(Long itemId, Long userId, Pageable pageable);

  Slice<BattleItemCommentResponse.Detail> findBattleComments(Long battleId, Long userId, Pageable pageable);
}
