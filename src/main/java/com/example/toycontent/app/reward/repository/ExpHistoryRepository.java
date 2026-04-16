package com.example.toycontent.app.reward.repository;

import com.example.toycontent.app.reward.domain.ExpHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpHistoryRepository extends JpaRepository<ExpHistory, Long> {

  Page<ExpHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
