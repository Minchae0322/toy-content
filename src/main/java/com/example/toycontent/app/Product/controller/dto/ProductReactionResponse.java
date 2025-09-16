package com.example.toycontent.app.Product.controller.dto;

import com.example.toycontent.app.Product.domain.ProductReaction;
import com.example.toycontent.app.common.enumuration.ReactionType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;

public abstract class ProductReactionResponse {

  @Data
  @Builder
  public static class ProductUserReaction {
    private Boolean hasLike;
    private Boolean hasBookmark;
    private Boolean hasInterest;
    private Boolean hasShare;
    private Boolean hasWish;

    public static ProductUserReaction of(List<ProductReaction> productReactions) {
      if (productReactions == null || productReactions.isEmpty()) {
        return ProductUserReaction.builder().build();
      }

      Set<ReactionType> activeReactions = productReactions.stream()
          .filter(ProductReaction::getIsActive)
          .map(ProductReaction::getReactionType)
          .collect(Collectors.toSet());

      return ProductUserReaction.builder()
          .hasLike(activeReactions.contains(ReactionType.LIKE))
          .hasBookmark(activeReactions.contains(ReactionType.BOOKMARK))
          .hasInterest(activeReactions.contains(ReactionType.INTEREST))
          .build();
    }

    public static ProductUserReaction createDefault() {
      return ProductUserReaction.builder()
          .hasLike(false)
          .hasBookmark(false)
          .hasInterest(false)
          .hasShare(false)
          .hasWish(false)
          .build();
    }

  }

}
