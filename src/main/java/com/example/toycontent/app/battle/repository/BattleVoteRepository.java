package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.BattleVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleVoteRepository extends JpaRepository<BattleVote, Long> {

}
