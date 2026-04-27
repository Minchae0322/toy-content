package com.example.toycontent.app.reward.exp.repository;

import com.example.toycontent.app.common.enumuration.ExpSource;
import com.example.toycontent.app.reward.exp.domain.ExpHistory;
import com.example.toycontent.app.reward.exp.repository.querydsl.ExpHistoryRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpHistoryRepository extends JpaRepository<ExpHistory, Long>,
    ExpHistoryRepositoryCustom {

  boolean existsByUserIdAndSourceAndSourceId(Long userId, ExpSource source, Long sourceId);

  long countByUserIdAndSourceAndSourceId(Long userId, ExpSource source, Long sourceId);
}
