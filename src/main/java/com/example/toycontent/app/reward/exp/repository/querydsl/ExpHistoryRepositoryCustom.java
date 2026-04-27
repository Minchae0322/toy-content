package com.example.toycontent.app.reward.exp.repository.querydsl;

import com.example.toycontent.app.reward.controller.dto.RewardResponse.ExpHistoryInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExpHistoryRepositoryCustom {

  Page<ExpHistoryInfo> findExpHistoryByUserId(Long userId, Pageable pageable);
}
