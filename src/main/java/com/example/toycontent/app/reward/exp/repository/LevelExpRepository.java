package com.example.toycontent.app.reward.exp.repository;

import com.example.toycontent.app.reward.exp.domain.LevelExp;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelExpRepository extends JpaRepository<LevelExp, Integer> {

  List<LevelExp> findAllByOrderByLevelAsc();
}
