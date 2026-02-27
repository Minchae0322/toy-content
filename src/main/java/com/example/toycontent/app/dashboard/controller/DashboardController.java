package com.example.toycontent.app.dashboard.controller;


import com.example.toycontent.app.dashboard.controller.dto.DashboardSummaryResponse;
import com.example.toycontent.app.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "DashboardController", description = "대시보드 API")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(value = "/dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

  @Operation(summary = "대시보드 요약 조회")
  @GetMapping("/summary")
  public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
    return ResponseEntity.ok(dashboardService.getDashboardSummary());
  }
}
