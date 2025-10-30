package com.example.toycontent.app.battle.repository.querydsl;

import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleList;
import com.example.toycontent.app.battle.controller.dto.BattleSearchCondition;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface BattleRepositoryCustom {

  List<BattleList> findBattlesWithSearchCondition(BattleSearchCondition condition, Pageable pageable);

  Long countBattlesWithSearchCondition(BattleSearchCondition condition);
}
