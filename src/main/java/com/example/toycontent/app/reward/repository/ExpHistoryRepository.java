package com.example.toycontent.app.reward.repository;

import com.example.toycontent.app.reward.domain.ExpHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpHistoryRepository extends JpaRepository<ExpHistory, Long> {
}
