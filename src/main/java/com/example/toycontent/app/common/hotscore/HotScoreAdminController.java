package com.example.toycontent.app.common.hotscore;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.HotScoreErrorCode;
import com.example.toycontent.app.common.hotscore.HotScoreAdminService.DivisorStatus;
import com.example.toycontent.app.common.hotscore.HotScoreAdminService.RecalculateResult;
import com.example.toycontent.app.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 핫 스코어 관리 (ADMIN 전용).
 *
 * <pre>
 *   GET  /admin/hot-score                        도메인별 시간 상수 현황
 *   PUT  /admin/hot-score/{domain}               상수 변경 + 그 도메인 전체 재계산
 *   POST /admin/hot-score/{domain}/recalculate   전체 재계산만 (배포 직후 옛 점수를 새 척도로 바꿀 때)
 * </pre>
 *
 * <p>평상시 점수는 참여가 생기는 행에서 자동 갱신되므로 이 API는 상수를 바꿀 때만 쓴다.
 * 동기 실행이라 재계산이 끝나야 응답한다. {@code domain}은 feed · battle · product.</p>
 */
@Tag(name = "HotScoreAdmin", description = "핫 스코어 시간 상수 · 전체 재계산 (ADMIN 전용)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/hot-score")
@PreAuthorize("hasAuthority('ADMIN')")
public class HotScoreAdminController {

  private final HotScoreAdminService hotScoreAdminService;

  @Operation(summary = "도메인별 시간 상수 현황")
  @GetMapping
  public ResponseEntity<ApiResponse<List<DivisorStatus>>> current() {
    return ResponseEntity.ok(ApiResponse.success(hotScoreAdminService.current()));
  }

  @Operation(summary = "시간 상수 변경 + 전체 재계산", description = "단위는 초. 1시간 ~ 1년.")
  @PutMapping("/{domain}")
  public ResponseEntity<ApiResponse<RecalculateResult>> changeDivisor(
      @PathVariable String domain, @RequestBody ChangeDivisorRequest request) {
    RecalculateResult result = hotScoreAdminService.changeDivisor(parse(domain), request.timeDivisorSeconds());
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @Operation(summary = "전체 재계산만", description = "상수는 그대로 두고 저장된 점수만 지금 상수로 다시 쓴다.")
  @PostMapping("/{domain}/recalculate")
  public ResponseEntity<ApiResponse<RecalculateResult>> recalculate(@PathVariable String domain) {
    return ResponseEntity.ok(ApiResponse.success(hotScoreAdminService.recalculate(parse(domain))));
  }

  private static HotScoreDomain parse(String domain) {
    try {
      return HotScoreDomain.valueOf(domain.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new RestApiException(HotScoreErrorCode.DOMAIN_NOT_FOUND);
    }
  }

  public record ChangeDivisorRequest(Long timeDivisorSeconds) {
  }
}
