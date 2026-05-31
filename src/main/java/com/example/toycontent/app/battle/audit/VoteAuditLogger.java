package com.example.toycontent.app.battle.audit;

import com.example.toycontent.app.battle.controller.dto.BattleVoteRequest;
import com.example.toycontent.app.common.voter.VoterId;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 투표 어뷰징 사후 분석용 로그. 별도 테이블 없이 로그 라인만 남긴다.
 *
 * <p>로거 이름 {@code vote-audit}을 별도로 두어 필요 시 logback에서 appender 분리 가능.
 * 게스트 dedup은 클라이언트 쿠키/localStorage 기반이라 캐시 삭제로 우회되므로 IP/UA를 함께 남긴다.
 */
@Component
public class VoteAuditLogger {

  private static final Logger AUDIT = LoggerFactory.getLogger("vote-audit");
  private static final String UNKNOWN = "-";

  public void logVote(Long battleId, VoterId voter, BattleVoteRequest.Vote request) {
    HttpServletRequest req = currentRequest();
    AUDIT.info("action=VOTE battleId={} userId={} guestId={} ip={} ua=\"{}\" items={}",
        battleId,
        voter.userId(),
        voter.guestId(),
        clientIp(req),
        userAgent(req),
        summarizeItems(request));
  }

  public void logCancelVote(Long battleItemId, VoterId voter) {
    HttpServletRequest req = currentRequest();
    AUDIT.info("action=CANCEL_VOTE itemId={} userId={} guestId={} ip={} ua=\"{}\"",
        battleItemId,
        voter.userId(),
        voter.guestId(),
        clientIp(req),
        userAgent(req));
  }

  private static String summarizeItems(BattleVoteRequest.Vote request) {
    if (request == null || request.getVotes() == null) {
      return "[]";
    }
    return request.getVotes().stream()
        .map(v -> v.getItemId() + ":" + v.getRank())
        .collect(Collectors.joining(",", "[", "]"));
  }

  private static HttpServletRequest currentRequest() {
    var attrs = RequestContextHolder.getRequestAttributes();
    return (attrs instanceof ServletRequestAttributes sra) ? sra.getRequest() : null;
  }

  /**
   * X-Forwarded-For 첫 번째 IP 우선 — nginx/ALB 등 reverse proxy 환경 대응.
   * 없으면 remoteAddr fallback.
   */
  private static String clientIp(HttpServletRequest req) {
    if (req == null) {
      return UNKNOWN;
    }
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    String remote = req.getRemoteAddr();
    return remote != null ? remote : UNKNOWN;
  }

  private static String userAgent(HttpServletRequest req) {
    if (req == null) {
      return UNKNOWN;
    }
    String ua = req.getHeader("User-Agent");
    return ua != null ? ua : UNKNOWN;
  }
}
