package com.example.toycontent.app.dashboard.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryResponse {
  private Long activeBattleCount;
  private Long totalItemCount;
  private Long popularFeedCount;
}
