package com.example.toycontent.app.common.voter;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.CommonErrorCode;

public record VoterId(Long userId, String guestId) {

  public static VoterId user(Long userId) {
    if (userId == null) {
      throw new RestApiException(CommonErrorCode.INVALID_PARAMETER);
    }
    return new VoterId(userId, null);
  }

  public static VoterId guest(String guestId) {
    if (guestId == null || guestId.isBlank()) {
      throw new RestApiException(CommonErrorCode.INVALID_PARAMETER);
    }
    return new VoterId(null, guestId);
  }

  public boolean isUser() {
    return userId != null;
  }

  public boolean isGuest() {
    return userId == null;
  }
}
