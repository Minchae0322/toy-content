package com.example.toycontent.app.battle.repository.querydsl;

import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleHotList;
import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleList;
import com.example.toycontent.app.battle.controller.dto.BattleSearchCondition;
import com.example.toycontent.app.battle.domain.Battle;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BattleRepositoryCustom {

  List<BattleList> findBattlesWithSearchCondition(BattleSearchCondition condition, Pageable pageable);

  Page<BattleHotList> findHotBattlesWithSearchCondition(Pageable pageable);
  Long countBattlesWithSearchCondition(BattleSearchCondition condition);



  List<Battle> findActiveAndUpcomingBattles();

  /** 핫 스코어 전체 재계산용 — 삭제되지 않은 배틀 전부 (종료된 배틀도 목록 정렬에 쓰이므로 포함) */
  List<Battle> findAllNotDeleted();
}
