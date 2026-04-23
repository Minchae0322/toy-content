package com.example.toycontent.app.reward.exp.repository;

import com.example.toycontent.app.common.enumuration.ExpSource;
import com.example.toycontent.app.reward.exp.domain.ExpHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpHistoryRepository extends JpaRepository<ExpHistory, Long> {

  Page<ExpHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  boolean existsByUserIdAndSourceAndSourceId(Long userId, ExpSource source, Long sourceId);

  long countByUserIdAndSourceAndSourceId(Long userId, ExpSource source, Long sourceId);
}
